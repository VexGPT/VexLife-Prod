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
    ): StageAdmission =
        synchronized(
            lock,
        ) {
            require(
                VexLifeAttachmentDraftFamily.resolve(
                    currentState = stateLocked(),
                    control = VexLifeAttachmentDraftFamily.AddFiles,
                ) !=
                    null,
            ) {
                "attachment draft is already at its file limit"
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
     * null means at least one selected file is not READY and Send must be held.
     */
    fun requestAttachments(): List<VexLifeLocalArtifact.RequestAttachment>? =
        synchronized(
            lock,
        ) {
            VexLifeAttachmentDraftFamily.requestAttachments(
                stateLocked(),
            )
        }

    /**
     * Clear only the exact READY draft that the completed Send consumed.
     *
     * A network replay that happens after the original draft was cleared cannot erase a newer draft.
     */
    fun clearCompletedSend(
        expected: List<VexLifeLocalArtifact.RequestAttachment>,
    ): Boolean =
        synchronized(
            lock,
        ) {
            val current =
                VexLifeAttachmentDraftFamily.requestAttachments(
                    stateLocked(),
                )
                    ?: return@synchronized false
            if (
                current !=
                expected
            ) {
                return@synchronized false
            }
            entries.clear()
            true
        }

    private fun requireStaging(
        draftRef: String,
    ): Entry {
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

    private fun stateLocked(): VexLifeAttachmentDraftFamily.State =
        VexLifeAttachmentDraftFamily.state(
            entries.values
                .map {
                    it.item
                },
        )
}

// [VXG RealForever]
