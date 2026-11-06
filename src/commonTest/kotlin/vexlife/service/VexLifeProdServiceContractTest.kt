package vexlife.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import vexlife.browser.VexLifeProdBrowserContract

class VexLifeProdServiceContractTest {
    @Test
    fun ordinaryServiceTopologyIsOneFixedSourceContract() {
        assertEquals(
            "127.0.0.1",
            VexLifeProdServiceContract.Host,
        )
        assertEquals(
            18_210,
            VexLifeProdServiceContract.Port,
        )
        assertEquals(
            "http://127.0.0.1:18210",
            VexLifeProdServiceContract.Origin,
        )
        assertEquals(
            18_211,
            VexLifeProdBrowserContract.FrameHostPort,
        )
        assertEquals(
            "http://127.0.0.1:18211",
            VexLifeProdBrowserContract.FrameHostOrigin,
        )
        assertEquals(
            "/",
            VexLifeProdServiceContract.PresentationIndexPath,
        )
        assertEquals(
            "/vexweb.js",
            VexLifeProdServiceContract.PresentationJavascriptPath,
        )
        assertEquals(
            "/api/v1/source-currentness",
            VexLifeProdServiceContract.SourceCurrentnessPath,
        )
        assertEquals(
            "/api/v1/conversation",
            VexLifeProdServiceContract.ConversationPath,
        )
        assertEquals(
            "/api/v1/conversation/send",
            VexLifeProdServiceContract.ConversationSendPath,
        )
    }

    @Test
    fun sendRequestKeepsBrowserContextOptionalAndBounded() {
        val request =
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "send.vexlife.browser.test.001",
                content = "Hello Vex",
            )

        assertEquals(
            "send.vexlife.browser.test.001",
            request.idempotencyKey,
        )
        assertEquals(
            "Hello Vex",
            request.content,
        )
        assertNull(
            request.browserContext,
        )

        val withContext =
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "send.vexlife.browser.test.002",
                content = "What is on this page?",
                browserContext =
                    VexLifeProdServiceContract.BrowserContextInput(
                        pageUrl = VexLifeProdBrowserContract.InitialPageUrl,
                        title = "Vextreme Terrain",
                        text = "Current direct-contact terrain text",
                        capture = VexLifeProdBrowserContract.CaptureRendered,
                    ),
            )
        assertEquals(
            VexLifeProdBrowserContract.InitialPageUrl,
            withContext.browserContext
                ?.pageUrl,
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "../escape",
                content = "Hello",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.BrowserContextInput(
                pageUrl = "http://127.0.0.1/private",
                title = null,
                text = "private",
                capture = VexLifeProdBrowserContract.CaptureRendered,
            )
        }
    }
}

// [VXG RealForever]
