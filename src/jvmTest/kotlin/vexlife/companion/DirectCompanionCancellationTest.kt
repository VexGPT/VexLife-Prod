package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectCompanionCancellationTest {
    @Test
    fun oneRequestCancelsTheBoundTurnAndRunsRequestLocalCleanupOnlyOnce() {
        val cancellation =
            DirectCompanionCancellation()
        var cleanupCount =
            0
        val cleanup =
            cancellation.onCancel {
                cleanupCount +=
                    1
            }

        assertFalse(
            cancellation.cancellationRequested,
        )
        assertTrue(
            cancellation.request(),
        )
        assertTrue(
            cancellation.cancellationRequested,
        )
        assertEquals(
            1,
            cleanupCount,
        )
        assertFalse(
            cancellation.request(),
            "repeated Stop observes the same cancellation rather than performing another effect",
        )
        assertEquals(
            1,
            cleanupCount,
        )
        assertFailsWith<DirectCompanionCancellation.Cancelled> {
            cancellation.checkpoint()
        }

        cleanup.close()
    }

    @Test
    fun cancellationIsRequestLocalAndDoesNotOwnAnyRuntimeProcessHandle() {
        assertTrue(
            DirectCompanionCancellation::class.java.declaredFields.none {
                field ->
                field.name.contains(
                    "process",
                    ignoreCase = true,
                ) ||
                    field.name.contains(
                        "runtime",
                        ignoreCase = true,
                    )
            },
        )
    }
}

// [VXG RealForever]
