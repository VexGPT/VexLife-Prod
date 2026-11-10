package vexlife.browser

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeProdFrameHostTest {
    @Test
    fun nestedSameOriginFrameContextRelaysThroughTheOuterTrustedFrame() {
        VexLifeProdFrameHost
            .startAt(
                InetSocketAddress(
                    "127.0.0.1",
                    0,
                ),
            )
            .use {
                host ->
                val script =
                    host.reporterScript(
                        "https://vgong24.github.io/Vextreme/pages/terrain-map.html",
                    )

                assertTrue(
                    script.contains(
                        "const FORWARD_TYPE = 'vexlife.browser-context-forward/v1'",
                    ),
                )
                assertTrue(
                    script.contains(
                        "if (window.parent === window.top)",
                    ),
                )
                assertTrue(
                    script.contains(
                        "event.origin !== location.origin",
                    ),
                )
                assertTrue(
                    script.contains(
                        "sendContextUpward(payload)",
                    ),
                )
            }
    }

    @Test
    fun admittedAbsoluteUrlsStayInsideTheIsolatedFrameProjection() {
        assertEquals(
            """
            {
              "liveUrl": "http://vgong24.github.io.vex.localhost:18211/Vextreme/pages/restoration-protocol.html",
              "workUrl": "http://vexgpt.github.io.vex.localhost:18211/VexWork/orientation.html",
              "external": "https://example.com/"
            }
            """.trimIndent(),
            VexLifeProdFrameHost.projectAdmittedAbsoluteUrls(
                """
                {
                  "liveUrl": "https://vgong24.github.io/Vextreme/pages/restoration-protocol.html",
                  "workUrl": "https://vexgpt.github.io/VexWork/orientation.html",
                  "external": "https://example.com/"
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun framePolicyAllowsOnlySameAdmittedFrameOriginNestingInsideTheProduct() {
        val policy =
            VexLifeProdFrameHost.FrameContentSecurityPolicy

        assertTrue(
            policy.contains(
                "frame-src 'self'",
            ),
        )
        assertTrue(
            policy.contains(
                "frame-ancestors 'self' http://127.0.0.1:18210",
            ),
        )
        assertFalse(
            policy.contains(
                "frame-src 'none'",
            ),
        )
        assertTrue(
            policy.contains(
                "form-action 'none'",
            ),
        )
        assertTrue(
            policy.contains(
                "object-src 'none'",
            ),
        )
    }

    private fun request(
        port: Int,
        method: String,
        host: String,
    ): String =
        Socket().use {
            socket ->
            socket.connect(
                InetSocketAddress(
                    "127.0.0.1",
                    port,
                ),
            )
            val request =
                "$method / HTTP/1.1\r\n" +
                    "Host: $host\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            socket.getOutputStream()
                .write(
                    request.toByteArray(
                        StandardCharsets.US_ASCII,
                    ),
                )
            socket.getOutputStream()
                .flush()

            requireNotNull(
                socket.getInputStream()
                    .bufferedReader(
                        StandardCharsets.UTF_8,
                    )
                    .readLine(),
            )
        }

    @Test
    fun listenerRejectsWriteMethodsAndUnadmittedFrameHostsBeforeUpstreamEffects() {
        VexLifeProdFrameHost
            .startAt(
                InetSocketAddress(
                    "127.0.0.1",
                    0,
                ),
            )
            .use {
                host ->
                val port =
                    URI.create(
                        host.origin,
                    )
                        .port

                assertTrue(
                    request(
                        port = port,
                        method = "POST",
                        host = "vgong24.github.io.vex.localhost:$port",
                    )
                        .startsWith(
                            "HTTP/1.1 403",
                        ),
                )
                assertTrue(
                    request(
                        port = port,
                        method = "GET",
                        host = "example.com.vex.localhost:$port",
                    )
                        .startsWith(
                            "HTTP/1.1 403",
                        ),
                )
            }
    }
}

// [VXG RealForever]
