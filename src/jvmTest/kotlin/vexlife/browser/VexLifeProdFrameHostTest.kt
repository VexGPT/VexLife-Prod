package vexlife.browser

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertTrue

class VexLifeProdFrameHostTest {
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
