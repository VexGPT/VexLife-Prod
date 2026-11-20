package vexlife.attachment

import vexlife.artifact.VexLifeLocalArtifact

/**
 * In-process custody of the person's current unsent attachment draft.
 *
 * Durable artifact bytes/projections live in [vexlife.artifact.VexLifeLocalArtifactStore]. This
 * runtime owns only the current Composer draft and in-flight staging cancellation/progress.
 */
class VexLifeAttachmentDraftRuntime {
    data class StageAdmission(
        val draftRef: String,
        val displayName: String,
        val totalBytesOrNull: Long?,
    )

    private data class Entry(
        var item: VexLifeAttachmentDraftFamily.Item,
        var cancellationRequested: Boolean = false,
    )

    private val lock =
        Any()

    private val entries =
        linkedMapOf<String, Entry>()

    /**
     * True only while one canonical Send owns the exact READY draft snapshot.
     *
     * This is execution custody, not durable attachment meaning. It closes the gap between the
     * service snapshot and ConversationTurn becoming Active so Add/Cancel/Remove cannot race a Send.
     */
    private var sendLeased =
        false

    fun sendLeased(): Boolean =
        synchronized(
            lock,
        ) {
            sendLeased
        }

    fun state(): VexLifeAttachmentDraftFamily.State =
        synchronized(
            lock,
        ) {
            stateLocked()
        }

    fun beginStage(
        draftRef: String,
        rawName: String,
        totalBytesOrNull: Long?,
    ): StageAdmission? =
        synchronized(
            lock,
        ) {
            if (
                sendLeased
            ) {
                return@synchronized null
            }
            if (
                VexLifeAttachmentDraftFamily.resolve(
                    currentState = stateLocked(),
                    control = VexLifeAttachmentDraftFamily.AddFiles,
                ) ==
                    null
            ) {
                return@synchronized null
            }
            require(
                draftRef !in
                    entries,
            ) {
                "attachment draft ref is already active"
            }
            val displayName =
                requireNotNull(
                    VexLifeLocalArtifact.admitDisplayName(
                        rawName,
                    ),
                ) {
                    "attachment file name is not admitted"
                }
            require(
                totalBytesOrNull ==
                    null ||
                    totalBytesOrNull in
                    1..VexLifeLocalArtifact.MaximumOriginalBytes.toLong(),
            ) {
                "attachment byte length is outside the admitted raw-artifact bound"
            }

            entries[
                draftRef
            ] =
                Entry(
                    item =
                        VexLifeAttachmentDraftFamily.Item(
                            draftRef = draftRef,
                            displayName = displayName,
                            status =
                                VexLifeAttachmentDraftFamily.ItemStatus.STAGING,
                            completedBytes = 0,
                            totalBytesOrNull = totalBytesOrNull,
                        ),
                )

            StageAdmission(
                draftRef = draftRef,
                displayName = displayName,
                totalBytesOrNull = totalBytesOrNull,
            )
        }

    fun progress(
        draftRef: String,
        completedBytes: Long,
    ) {
        synchronized(
            lock,
        ) {
            val entry =
                requireNotNull(
                    entries[
                        draftRef
                    ],
                ) {
                    "attachment stage is not active"
                }
            require(
                entry.item.status ==
                    VexLifeAttachmentDraftFamily.ItemStatus.STAGING,
            ) {
                "attachment progress requires one staging item"
            }
            require(
                completedBytes >=
                    entry.item.completedBytes,
            ) {
                "attachment progress moved backward"
            }
            entry.item =
                entry.item.copy(
                    completedBytes = completedBytes,
                    totalBytesOrNull =
                        maxOf(
                            entry.item.totalBytesOrNull
                                ?: 0,
                            completedBytes,
                        )
                            .takeIf {
                                it >
                                    0
                            },
                )
        }
    }

    fun cancelStage(
        draftRef: String,
    ): Boolean =
        synchronized(
            lock,
        ) {
            if (
                sendLeased
            ) {
                return@synchronized false
            }
            val entry =
                entries[
                    draftRef
                ]
                    ?: return@synchronized false
            if (
                entry.item.status !=
                VexLifeAttachmentDraftFamily.ItemStatus.STAGING
            ) {
                return@synchronized false
            }
            entry.cancellationRequested =
                true
            true
        }

    fun cancellationRequested(
        draftRef: String,
    ): Boolean =
        synchronized(
            lock,
        ) {
            entries[
                draftRef
            ]
                ?.cancellationRequested ==
                true
        }

    fun cancelled(
        draftRef: String,
    ): Boolean =
        synchronized(
            lock,
        ) {
            val entry =
                entries[
                    draftRef
                ]
                    ?: return@synchronized false
            if (
                entry.item.status !=
                    VexLifeAttachmentDraftFamily.ItemStatus.STAGING
            ) {
                return@synchronized false
            }
            entries.remove(
                draftRef,
            )
            true
        }

    fun ready(
        draftRef: String,
        artifactRef: String,
        projectionRef: String,
        completedBytes: Long,
    ): VexLifeAttachmentDraftFamily.Item =
        synchronized(
            lock,
        ) {
            val entry =
                requireStaging(
                    draftRef,
                )
            require(
                !entry.cancellationRequested,
            ) {
                "cancelled attachment stage cannot become ready"
            }
            val ready =
                entry.item.copy(
                    status =
                        VexLifeAttachmentDraftFamily.ItemStatus.READY,
                    artifactRef = artifactRef,
                    projectionRef = projectionRef,
                    completedBytes = completedBytes,
                    totalBytesOrNull =
                        maxOf(
                            entry.item.totalBytesOrNull
                                ?: 0,
                            completedBytes,
                        ),
                )
            entry.item =
                ready
            ready
        }

    fun held(
        draftRef: String,
        artifactRefOrNull: String?,
        reasonKey: String,
        completedBytes: Long,
    ): VexLifeAttachmentDraftFamily.Item =
        synchronized(
            lock,
        ) {
            val entry =
                requireStaging(
                    draftRef,
                )
            val held =
                entry.item.copy(
                    status =
                        VexLifeAttachmentDraftFamily.ItemStatus.HELD,
                    artifactRef = artifactRefOrNull,
                    projectionRef = null,
                    completedBytes = completedBytes,
                    totalBytesOrNull =
                        maxOf(
                            entry.item.totalBytesOrNull
                                ?: 0,
                            completedBytes,
                        )
                            .takeIf {
                                it >
                                    0
                            },
                    holdReasonKeyOrNull = reasonKey,
                )
            entry.item =
                held
            held
        }

    fun remove(
        draftRef: String,
    ): Boolean =
        synchronized(
            lock,
        ) {
            if (
                sendLeased
            ) {
                return@synchronized false
            }
            val entry =
                entries[
                    draftRef
                ]
                    ?: return@synchronized false
            if (
                entry.item.status ==
                    VexLifeAttachmentDraftFamily.ItemStatus.STAGING
            ) {
                return@synchronized false
            }
            entries.remove(
                draftRef,
            )
            true
        }

    /**
     * Acquire the exact READY draft for one canonical Send.
     *
     * null means the draft is staging/held or another Send already owns it.
     */
    fun beginSend(): List<VexLifeLocalArtifact.RequestAttachment>? =
        synchronized(
            lock,
        ) {
            if (
                sendLeased
            ) {
                return@synchronized null
            }
            val snapshot =
                VexLifeAttachmentDraftFamily.requestAttachments(
                    stateLocked(),
                )
                    ?: return@synchronized null
            sendLeased =
                true
            snapshot
        }

    /**
     * Clear and release only the exact draft owned by the successful Send.
     */
    fun completeSend(
        expected: List<VexLifeLocalArtifact.RequestAttachment>,
    ): Boolean =
        synchronized(
            lock,
        ) {
            if (
                !sendLeased
            ) {
                return@synchronized false
            }
            requireCurrentSendSnapshot(
                expected,
            )
            entries.clear()
            sendLeased =
                false
            true
        }

    /**
     * Release a failed/stopped/unrealized Send while preserving its exact draft.
     */
    fun releaseSend(
        expected: List<VexLifeLocalArtifact.RequestAttachment>,
    ): Boolean =
        synchronized(
            lock,
        ) {
            if (
                !sendLeased
            ) {
                return@synchronized false
            }
            requireCurrentSendSnapshot(
                expected,
            )
            sendLeased =
                false
            true
        }

    private fun requireStaging(
        draftRef: String,
    ): Entry {
        require(
            !sendLeased,
        ) {
            "attachment draft is leased by one canonical Send"
        }
        val entry =
            requireNotNull(
                entries[
                    draftRef
                ],
            ) {
                "attachment stage is not active"
            }
        require(
            entry.item.status ==
                VexLifeAttachmentDraftFamily.ItemStatus.STAGING,
        ) {
            "attachment stage is no longer staging"
        }
        return entry
    }

    private fun requireCurrentSendSnapshot(
        expected: List<VexLifeLocalArtifact.RequestAttachment>,
    ) {
        val current =
            VexLifeAttachmentDraftFamily.requestAttachments(
                stateLocked(),
            )
        require(
            current ==
                expected,
        ) {
            "canonical Send attachment snapshot changed while leased"
        }
    }

    private fun stateLocked(): VexLifeAttachmentDraftFamily.State =
        VexLifeAttachmentDraftFamily.state(
            entries.values
                .map {
                    it.item
                },
        )
}

// [VXG RealForever]
