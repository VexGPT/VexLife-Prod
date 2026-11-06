package vexlife.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun sendRequestCarriesOnlyIdempotencyIdentityAndHumanContent() {
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

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdServiceContract.SendRequest(
                idempotencyKey = "../escape",
                content = "Hello",
            )
        }
    }
}

// [VXG RealForever]
