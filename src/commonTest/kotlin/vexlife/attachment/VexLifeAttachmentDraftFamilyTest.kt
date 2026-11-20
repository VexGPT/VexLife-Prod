package vexlife.attachment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact

class VexLifeAttachmentDraftFamilyTest {
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
    fun emptyDraftOffersAddWithoutInventingRequestResources() {
        val state =
            VexLifeAttachmentDraftFamily.Empty
        val display =
            VexLifeAttachmentDraftFamily.displayStateFor(
                state,
            )

        assertEquals(
            VexLifeAttachmentDraftFamily.ControlAddFiles,
            display.addControlRef,
        )
        assertEquals(
            "workspace.button.attach",
            display.addLabel?.key,
        )
        assertTrue(
            display.items.isEmpty(),
        )
        assertTrue(
            display.requestResourcesReady,
            "an empty draft is sendable without any resource binding",
        )
        assertEquals(
            emptyList(),
            VexLifeAttachmentDraftFamily.requestAttachments(
                state,
            ),
        )

        val act =
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.AddFiles,
            )
        assertIs<VexLifeAttachmentDraftFamily.Effect.RequestFiles>(
            act?.effect,
        )
    }

    @Test
    fun stagingItemMayCancelButCannotBeSilentlyOmittedAtSend() {
        val state =
            VexLifeAttachmentDraftFamily.state(
                listOf(
                    VexLifeAttachmentDraftFamily.Item(
                        draftRef = "draft.attachment.001",
                        displayName = "notes.txt",
                        status =
                            VexLifeAttachmentDraftFamily.ItemStatus.STAGING,
                        completedBytes = 12,
                        totalBytesOrNull = 100,
                    ),
                ),
            )

        assertNull(
            VexLifeAttachmentDraftFamily.requestAttachments(
                state,
            ),
            "staging resource must hold Send rather than disappear from the request",
        )
        assertNull(
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.Remove(
                        "draft.attachment.001",
                    ),
            ),
        )

        val cancel =
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.CancelStage(
                        "draft.attachment.001",
                    ),
            )
        assertEquals(
            VexLifeAttachmentDraftFamily.Effect.CancelStage(
                "draft.attachment.001",
            ),
            cancel?.effect,
        )
        assertNull(
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.CancelStage(
                        "draft.attachment.other",
                    ),
            ),
            "a cancellation control must bind the exact staged item",
        )

        val display =
            VexLifeAttachmentDraftFamily.displayStateFor(
                state,
            )
        assertEquals(
            "workspace.status.attachment-staging",
            display.items.single().status.key,
        )
        assertEquals(
            VexLifeAttachmentDraftFamily.ControlCancelStage,
            display.items.single().controlRef,
        )
    }

    @Test
    fun readyItemProjectsOneExactRequestAttachmentAndMayBeRemovedFromDraft() {
        val ready =
            VexLifeAttachmentDraftFamily.Item(
                draftRef = "draft.attachment.ready.001",
                displayName = "report.txt",
                status =
                    VexLifeAttachmentDraftFamily.ItemStatus.READY,
                artifactRef = artifactRef,
                projectionRef = projectionRef,
                completedBytes = 42,
                totalBytesOrNull = 42,
            )
        val state =
            VexLifeAttachmentDraftFamily.state(
                listOf(
                    ready,
                ),
            )

        val requestResources =
            requireNotNull(
                VexLifeAttachmentDraftFamily.requestAttachments(
                    state,
                ),
            )
        assertEquals(
            listOf(
                VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = artifactRef,
                    projectionRef = projectionRef,
                    displayName = "report.txt",
                ),
            ),
            requestResources,
        )

        val remove =
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.Remove(
                        ready.draftRef,
                    ),
            )
        assertEquals(
            VexLifeAttachmentDraftFamily.Effect.RemoveFromDraft(
                ready.draftRef,
            ),
            remove?.effect,
        )
        assertNull(
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.CancelStage(
                        ready.draftRef,
                    ),
            ),
        )

        val display =
            VexLifeAttachmentDraftFamily.displayStateFor(
                state,
            )
        assertTrue(
            display.requestResourcesReady,
        )
        assertEquals(
            "workspace.status.attachment-ready",
            display.items.single().status.key,
        )
        assertEquals(
            VexLifeAttachmentDraftFamily.ControlRemove,
            display.items.single().controlRef,
        )
    }

    @Test
    fun heldItemPreventsSendUntilThePersonRemovesOrRepairsIt() {
        val state =
            VexLifeAttachmentDraftFamily.state(
                listOf(
                    VexLifeAttachmentDraftFamily.Item(
                        draftRef = "draft.attachment.held.001",
                        displayName = "unsupported.bin",
                        status =
                            VexLifeAttachmentDraftFamily.ItemStatus.HELD,
                        holdReasonKeyOrNull =
                            "workspace.status.attachment-refused",
                    ),
                ),
            )

        assertNull(
            VexLifeAttachmentDraftFamily.requestAttachments(
                state,
            ),
        )
        val display =
            VexLifeAttachmentDraftFamily.displayStateFor(
                state,
            )
        assertEquals(
            "workspace.status.attachment-refused",
            display.items.single().status.key,
        )
        assertEquals(
            VexLifeAttachmentDraftFamily.ControlRemove,
            display.items.single().controlRef,
        )
    }

    @Test
    fun eightItemsAreAnExactDraftLimitAndHideAdd() {
        val items =
            (1..VexLifeLocalArtifact.MaximumPerRequest).map {
                index ->
                VexLifeAttachmentDraftFamily.Item(
                    draftRef =
                        "draft.attachment.limit." +
                            index,
                    displayName =
                        "file-" +
                            index +
                            ".txt",
                    status =
                        VexLifeAttachmentDraftFamily.ItemStatus.READY,
                    artifactRef = artifactRef,
                    projectionRef = projectionRef,
                    completedBytes = 1,
                    totalBytesOrNull = 1,
                )
            }
        val state =
            VexLifeAttachmentDraftFamily.state(
                items,
            )
        val display =
            VexLifeAttachmentDraftFamily.displayStateFor(
                state,
            )

        assertNull(
            display.addControlRef,
        )
        assertEquals(
            "workspace.status.attachment-limit",
            display.limitStatus?.key,
        )
        assertNull(
            VexLifeAttachmentDraftFamily.resolve(
                currentState = state,
                control =
                    VexLifeAttachmentDraftFamily.AddFiles,
            ),
        )
        assertEquals(
            VexLifeLocalArtifact.MaximumPerRequest,
            requireNotNull(
                VexLifeAttachmentDraftFamily.requestAttachments(
                    state,
                ),
            ).size,
        )
    }
}

// [VXG RealForever]
