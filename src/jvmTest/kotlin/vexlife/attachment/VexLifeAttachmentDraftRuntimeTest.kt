package vexlife.attachment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
            runtime.beginSend(),
            "a staging item must hold Send",
        )
        assertFalse(
            runtime.sendLeased(),
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
                runtime.beginSend(),
            )
        assertEquals(
            1,
            snapshot.size,
        )
        assertTrue(
            runtime.sendLeased(),
        )
        assertFalse(
            runtime.remove(
                "draft.attachment.runtime.ready.001",
            ),
            "a leased Send makes its draft immutable until terminal outcome",
        )
        assertNull(
            runtime.beginStage(
                draftRef = "draft.attachment.runtime.new.002",
                rawName = "new.txt",
                totalBytesOrNull = 1,
            ),
            "Add Files cannot race the snapshot-to-turn boundary",
        )

        assertFailsWith<IllegalArgumentException> {
            runtime.completeSend(
                listOf(
                    snapshot.single()
                        .copy(
                            displayName = "another-name.txt",
                        ),
                ),
            )
        }
        assertTrue(
            runtime.sendLeased(),
            "a mismatched completion must not silently release the exact Send lease",
        )
        assertEquals(
            1,
            runtime.state()
                .items
                .size,
        )

        assertTrue(
            runtime.completeSend(
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
            runtime.beginSend(),
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
    @Test
    fun failedSendReleasesItsExactLeaseWithoutClearingTheDraft() {
        val runtime =
            VexLifeAttachmentDraftRuntime()
        runtime.beginStage(
            draftRef = "draft.attachment.runtime.failure.001",
            rawName = "keep-on-failure.txt",
            totalBytesOrNull = 4,
        )
        runtime.progress(
            draftRef = "draft.attachment.runtime.failure.001",
            completedBytes = 4,
        )
        runtime.ready(
            draftRef = "draft.attachment.runtime.failure.001",
            artifactRef = artifactRef,
            projectionRef = projectionRef,
            completedBytes = 4,
        )

        val snapshot =
            requireNotNull(
                runtime.beginSend(),
            )
        assertTrue(
            runtime.releaseSend(
                snapshot,
            ),
        )
        assertFalse(
            runtime.sendLeased(),
        )
        assertEquals(
            "keep-on-failure.txt",
            runtime.state()
                .items
                .single()
                .displayName,
        )
        assertTrue(
            runtime.remove(
                "draft.attachment.runtime.failure.001",
            ),
            "after terminal failure/Stop the human may edit the returned draft again",
        )
    }

}

// [VXG RealForever]
