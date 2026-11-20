package vexlife.attachment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact

class VexLifeAttachmentDraftRuntimeTest {
    private val artifactRef =
        VexLifeLocalArtifact.refFor(
            "a".repeat(
                64,
            ),
        )
    private val projectionRef =
        VexLifeLocalArtifact.ProjectionRefPrefix +
            "b".repeat(
                64,
            )

    @Test
    fun stagingProgressMayBeCancelledAndThenLeavesNoDraftResource() {
        val runtime =
            VexLifeAttachmentDraftRuntime()

        runtime.beginStage(
            draftRef = "draft.attachment.runtime.001",
            rawName = "/tmp/notes.txt",
            totalBytesOrNull = 100,
        )
        runtime.progress(
            draftRef = "draft.attachment.runtime.001",
            completedBytes = 40,
        )

        val staging =
            runtime.state()
                .items
                .single()
        assertEquals(
            "notes.txt",
            staging.displayName,
        )
        assertEquals(
            40,
            staging.completedBytes,
        )
        assertNull(
            runtime.requestAttachments(),
            "a staging item must hold Send",
        )

        assertTrue(
            runtime.cancelStage(
                "draft.attachment.runtime.001",
            ),
        )
        assertTrue(
            runtime.cancellationRequested(
                "draft.attachment.runtime.001",
            ),
        )
        assertTrue(
            runtime.cancelled(
                "draft.attachment.runtime.001",
            ),
        )
        assertTrue(
            runtime.state()
                .items
                .isEmpty(),
        )
    }

    @Test
    fun readySnapshotClearsOnlyAfterTheExactCompletedSend() {
        val runtime =
            VexLifeAttachmentDraftRuntime()
        runtime.beginStage(
            draftRef = "draft.attachment.runtime.ready.001",
            rawName = "report.txt",
            totalBytesOrNull = 10,
        )
        runtime.progress(
            draftRef = "draft.attachment.runtime.ready.001",
            completedBytes = 10,
        )
        runtime.ready(
            draftRef = "draft.attachment.runtime.ready.001",
            artifactRef = artifactRef,
            projectionRef = projectionRef,
            completedBytes = 10,
        )

        val snapshot =
            requireNotNull(
                runtime.requestAttachments(),
            )
        assertEquals(
            1,
            snapshot.size,
        )

        assertFalse(
            runtime.clearCompletedSend(
                listOf(
                    snapshot.single()
                        .copy(
                            displayName = "another-name.txt",
                        ),
                ),
            ),
            "a different/newer draft must never be cleared by a prior Send",
        )
        assertEquals(
            1,
            runtime.state()
                .items
                .size,
        )

        assertTrue(
            runtime.clearCompletedSend(
                snapshot,
            ),
        )
        assertTrue(
            runtime.state()
                .items
                .isEmpty(),
        )
    }

    @Test
    fun heldItemMayBeRemovedButCannotBecomeARequestResource() {
        val runtime =
            VexLifeAttachmentDraftRuntime()
        runtime.beginStage(
            draftRef = "draft.attachment.runtime.held.001",
            rawName = "binary.dat",
            totalBytesOrNull = 4,
        )
        runtime.progress(
            draftRef = "draft.attachment.runtime.held.001",
            completedBytes = 4,
        )
        runtime.held(
            draftRef = "draft.attachment.runtime.held.001",
            artifactRefOrNull = artifactRef,
            reasonKey = "workspace.status.attachment-refused",
            completedBytes = 4,
        )

        assertNull(
            runtime.requestAttachments(),
        )
        assertTrue(
            runtime.remove(
                "draft.attachment.runtime.held.001",
            ),
        )
        assertTrue(
            runtime.state()
                .items
                .isEmpty(),
        )
    }
}

// [VXG RealForever]
