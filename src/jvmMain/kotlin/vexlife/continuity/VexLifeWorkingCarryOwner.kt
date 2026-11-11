package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Durable custody for one current Vex present working carry.
 *
 * Gradual-aging refresh may be prepared before provider invocation so the same turn can use the
 * candidate, but currentness advances only after the exact matching Conversation turn completes.
 *
 * Rollover-handoff refresh is formed only after a completed high-pressure turn and may advance
 * currentness only while that completed head remains the exact current Conversation edge.
 *
 * Durable bytes here are working-state custody, not semantic Memory acceptance.
 */
class VexLifeWorkingCarryOwner(
    productHome: Path,
) {
    companion object {
        const val RevisionSchema =
            "vexlife-prod.working-carry-revision/v3"

        const val FormationBindingSchema =
            "vexlife-prod.working-carry-formation-binding/v1"

        const val HeadSchema =
            "vexlife-prod.working-carry-head/v1"

        const val OwnerRef =
            "owner.vexlife-prod.working-carry.001"
    }

    @Serializable
    @ConsistentCopyVisibility
    data class FormationBinding internal constructor(
        val schemaVersion: String,
        val formationRef: String,
        val formationFingerprint: String,
        val refreshInputSha256: String,
        val draftSha256: String,
        val runtimeObservationRef: String,
        val runtimeObservationFingerprint: String,
        val invocationRef: String,
        val invocationFingerprint: String,
        val providerOutputSha256: String,
    ) {
        init {
            require(
                schemaVersion ==
                    FormationBindingSchema &&
                    formationFingerprint.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    formationRef ==
                    "working-carry-formation." +
                    formationFingerprint.take(
                        32,
                    ) &&
                    refreshInputSha256.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    draftSha256.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    runtimeObservationRef.isNotBlank() &&
                    runtimeObservationFingerprint.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    invocationRef.isNotBlank() &&
                    invocationFingerprint.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    providerOutputSha256.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ),
            ) {
                "working carry formation binding is invalid"
            }
        }
    }

    @Serializable
    private data class RevisionCore(
        val schemaVersion: String,
        val truthClass: String,
        val ownerRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val refreshMoment: String,
        val carry: VexLifeWorkingCarryContract.Draft,
        val formationBinding: FormationBinding,
        val priorRevisionRefOrNull: String?,
        val priorRevisionFingerprintOrNull: String?,
        val sourceConversationHeadSha256: String,
        val sourceTurnRef: String,
        val sourceRequestEventRef: String,
        val sourceRequestEventHash: String,
        val sourceRequestSequence: Long,
        val sourceResponseEventHash: String,
        val formedAt: String,
    )

    @Serializable
    data class Revision internal constructor(
        val schemaVersion: String,
        val truthClass: String,
        val ownerRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val refreshMoment: String,
        val carry: VexLifeWorkingCarryContract.Draft,
        val formationBinding: FormationBinding,
        val priorRevisionRefOrNull: String?,
        val priorRevisionFingerprintOrNull: String?,
        val sourceConversationHeadSha256: String,
        val sourceTurnRef: String,
        val sourceRequestEventRef: String,
        val sourceRequestEventHash: String,
        val sourceRequestSequence: Long,
        val sourceResponseEventHash: String,
        val formedAt: String,
        val revisionRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    private data class HeadCore(
        val schemaVersion: String,
        val truthClass: String,
        val ownerRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val revisionRef: String,
        val revisionFingerprint: String,
        val committedAt: String,
    )

    @Serializable
    private data class Head(
        val schemaVersion: String,
        val truthClass: String,
        val ownerRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val revisionRef: String,
        val revisionFingerprint: String,
        val committedAt: String,
        val semanticFingerprint: String,
    )

    data class Current(
        val revision: Revision,
    )

    @ConsistentCopyVisibility
    data class Prepared internal constructor(
        val companionLineageRef: String,
        val threadRef: String,
        val priorRevisionRefOrNull: String?,
        val priorRevisionFingerprintOrNull: String?,
        val priorConversationHeadSha256: String?,
        val currentRequest: DirectConversationStore.Event,
        val refreshInput: VexLifeWorkingCarryContract.RefreshInput,
        val carry: VexLifeWorkingCarryContract.Draft,
        val formationBinding: FormationBinding,
    )

    data class CommitResult(
        val state: String,
        val current: Current,
    )

    private data class Paths(
        val root: Path,
        val revisions: Path,
        val head: Path,
        val lock: Path,
    )

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    private val manifest =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                home,
            ),
        ) {
            "working carry owner requires one current canonical VexLife Home"
        }
            .manifest

    fun readCurrent(
        companionLineageRef: String =
            manifest.currentCompanionLineageRef,
        threadRef: String =
            DirectConversationIdentity.ThreadRef,
    ): Current? {
        requireIdentity(
            companionLineageRef = companionLineageRef,
            threadRef = threadRef,
        )
        val paths =
            paths(
                companionLineageRef,
                threadRef,
            )
        if (
            VexOwnedPath.observeDirectory(
                home,
                paths.root,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        val head =
            VexDurableContractFile.read<Head>(
                ownedRoot = home,
                path = paths.head,
            )
                ?: return null
        validateHead(
            head,
        )
        val revision =
            requireNotNull(
                VexDurableContractFile.read<Revision>(
                    ownedRoot = home,
                    path =
                        paths.revisions.resolve(
                            head.revisionFingerprint +
                                ".json",
                        ),
                ),
            ) {
                "working carry current head revision is missing"
            }
                .also(
                    ::validateRevision,
                )

        require(
            revision.revisionRef ==
                head.revisionRef &&
                revision.semanticFingerprint ==
                head.revisionFingerprint &&
                revision.companionLineageRef ==
                head.companionLineageRef &&
                revision.threadRef ==
                head.threadRef &&
                revision.formedAt ==
                head.committedAt,
        ) {
            "working carry current head does not bind its exact immutable revision"
        }

        return Current(
            revision = revision,
        )
    }

    internal fun prepare(
        priorConversationHeadSha256: String?,
        currentRequest: DirectConversationStore.Event,
        refreshInput: VexLifeWorkingCarryContract.RefreshInput,
        carry: VexLifeWorkingCarryContract.Draft,
        formationBinding: FormationBinding,
    ): Prepared {
        requireRequest(
            currentRequest,
        )
        require(
            currentRequest.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                currentRequest.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "working carry prepare request is outside the current Product lineage/thread"
        }
        require(
            (
                currentRequest.sequence ==
                    0L &&
                    priorConversationHeadSha256 ==
                    null
            ) ||
                (
                    currentRequest.sequence >
                        0L &&
                        priorConversationHeadSha256 !=
                        null
                ),
        ) {
            "working carry prepare prior Conversation head does not match request position"
        }
        priorConversationHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "priorConversationHeadSha256",
                )
            }

        val input =
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                refreshInput,
            )
        val output =
            VexLifeWorkingCarryContract.requireDraftFromRefresh(
                refresh = input,
                draft = carry,
            )
        requireRefreshRequestMatches(
            refresh = input,
            currentRequest = currentRequest,
        )
        require(
            input.companionLineageRef ==
                currentRequest.companionLineageRef &&
                input.threadRef ==
                currentRequest.threadRef,
        ) {
            "working carry refresh identity differs from the exact durable request"
        }
        requireFormationBinding(
            formationBinding,
        )
        require(
            formationBinding.refreshInputSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        input,
                    ),
                ) &&
                formationBinding.draftSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        output,
                    ),
                ),
        ) {
            "working carry formation binding does not belong to the exact refresh input and Draft"
        }

        val current =
            readCurrent(
                companionLineageRef = currentRequest.companionLineageRef,
                threadRef = currentRequest.threadRef,
            )

        if (
            current ==
                null
        ) {
            require(
                input.previousCarry ==
                    null,
            ) {
                "working carry refresh supplies a previous carry while no current carry exists"
            }
        } else {
            require(
                input.previousCarry ==
                    current.revision.carry,
            ) {
                "working carry refresh previous carry is not the exact owner-current carry"
            }
        }

        return Prepared(
            companionLineageRef = currentRequest.companionLineageRef,
            threadRef = currentRequest.threadRef,
            priorRevisionRefOrNull =
                current
                    ?.revision
                    ?.revisionRef,
            priorRevisionFingerprintOrNull =
                current
                    ?.revision
                    ?.semanticFingerprint,
            priorConversationHeadSha256 = priorConversationHeadSha256,
            currentRequest = currentRequest,
            refreshInput = input,
            carry = output,
            formationBinding = formationBinding,
        )
    }

    internal fun commitCompletedTurn(
        prepared: Prepared,
        completedHead: DirectConversationStore.Head,
    ): CommitResult {
        validatePrepared(
            prepared,
        )
        requireCompletedHead(
            prepared = prepared,
            completedHead = completedHead,
        )
        val candidate =
            formRevision(
                prepared = prepared,
                completedHead = completedHead,
            )
        val paths =
            paths(
                prepared.companionLineageRef,
                prepared.threadRef,
            )

        VexOwnedPath.ensureDirectory(
            home,
            paths.root,
        )

        return VexExclusiveFileLease
            .acquire(
                paths.lock,
            )
            .use {
                val current =
                    readCurrent(
                        companionLineageRef = prepared.companionLineageRef,
                        threadRef = prepared.threadRef,
                    )

                if (
                    current
                        ?.revision
                        ?.semanticFingerprint ==
                        candidate.semanticFingerprint
                ) {
                    return@use CommitResult(
                        state = "IDEMPOTENT_CURRENT",
                        current =
                            requireNotNull(
                                current,
                            ),
                    )
                }

                requirePriorStillCurrent(
                    current = current,
                    prepared = prepared,
                )
                writeImmutableOrRequireExact(
                    path =
                        paths.revisions.resolve(
                            candidate.semanticFingerprint +
                                ".json",
                        ),
                    value = candidate,
                )
                val head =
                    formHead(
                        revision = candidate,
                    )
                VexDurableContractFile.write(
                    ownedRoot = home,
                    path = paths.head,
                    value = head,
                )

                val rebound =
                    requireNotNull(
                        readCurrent(
                            companionLineageRef = prepared.companionLineageRef,
                            threadRef = prepared.threadRef,
                        ),
                    ) {
                        "working carry revision did not become current"
                    }
                require(
                    rebound.revision ==
                        candidate,
                ) {
                    "working carry current readback differs from the committed revision"
                }

                CommitResult(
                    state = "COMMITTED",
                    current = rebound,
                )
            }
    }

    internal fun commitCompletedRolloverHandoff(
        completedHead: DirectConversationStore.Head,
        refreshInput: VexLifeWorkingCarryContract.RolloverRefreshInput,
        carry: VexLifeWorkingCarryContract.Draft,
        formationBinding: FormationBinding,
    ): CommitResult {
        require(
            completedHead.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                completedHead.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "working carry rollover handoff completed head is outside the current Product lineage/thread"
        }
        requireSha256(
            completedHead.conversationHeadSha256,
            "completedHead.conversationHeadSha256",
        )
        val currentConversationHead =
            requireNotNull(
                DirectConversationStore(
                    home,
                )
                    .current()
                    .head,
            ) {
                "working carry rollover handoff requires one current completed Conversation head"
            }
        require(
            currentConversationHead ==
                completedHead,
        ) {
            "working carry rollover handoff completed head is no longer Conversation-current"
        }

        val input =
            VexLifeWorkingCarryContract.requireRolloverRefreshInputShape(
                refreshInput,
            )
        val output =
            VexLifeWorkingCarryContract.requireDraftFromRolloverRefresh(
                refresh = input,
                draft = carry,
            )
        require(
            input.companionLineageRef ==
                completedHead.companionLineageRef &&
                input.threadRef ==
                completedHead.threadRef,
        ) {
            "working carry rollover refresh identity differs from the exact completed Conversation head"
        }
        requireRolloverRefreshMatchesCompletedHead(
            refresh = input,
            completedHead = completedHead,
        )
        requireFormationBinding(
            formationBinding,
        )
        require(
            formationBinding.refreshInputSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        input,
                    ),
                ) &&
                formationBinding.draftSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        output,
                    ),
                ),
        ) {
            "working carry rollover formation binding does not belong to the exact refresh input and Draft"
        }

        val paths =
            paths(
                completedHead.companionLineageRef,
                completedHead.threadRef,
            )
        VexOwnedPath.ensureDirectory(
            home,
            paths.root,
        )

        return VexExclusiveFileLease
            .acquire(
                paths.lock,
            )
            .use {
                val current =
                    readCurrent(
                        companionLineageRef = completedHead.companionLineageRef,
                        threadRef = completedHead.threadRef,
                    )

                val currentRevision =
                    current
                        ?.revision
                if (
                    currentRevision !=
                        null &&
                    currentRevision.refreshMoment ==
                        VexLifeWorkingCarryContract.RolloverHandoff &&
                    currentRevision.sourceConversationHeadSha256 ==
                        completedHead.conversationHeadSha256 &&
                    currentRevision.carry ==
                        output &&
                    currentRevision.formationBinding ==
                        formationBinding
                ) {
                    return@use CommitResult(
                        state = "IDEMPOTENT_CURRENT",
                        current =
                            requireNotNull(
                                current,
                            ),
                    )
                }

                if (
                    current ==
                        null
                ) {
                    require(
                        input.previousCarry ==
                            null,
                    ) {
                        "working carry rollover refresh supplies a previous carry while no current carry exists"
                    }
                } else {
                    require(
                        input.previousCarry ==
                            current.revision.carry,
                    ) {
                        "working carry rollover refresh previous carry is not the exact owner-current carry"
                    }
                }

                val candidate =
                    formRolloverRevision(
                        current = current,
                        refresh = input,
                        carry = output,
                        formationBinding = formationBinding,
                        completedHead = completedHead,
                    )
                writeImmutableOrRequireExact(
                    path =
                        paths.revisions.resolve(
                            candidate.semanticFingerprint +
                                ".json",
                        ),
                    value = candidate,
                )
                val head =
                    formHead(
                        revision = candidate,
                    )
                VexDurableContractFile.write(
                    ownedRoot = home,
                    path = paths.head,
                    value = head,
                )

                val rebound =
                    requireNotNull(
                        readCurrent(
                            companionLineageRef = completedHead.companionLineageRef,
                            threadRef = completedHead.threadRef,
                        ),
                    ) {
                        "working carry rollover revision did not become current"
                    }
                require(
                    rebound.revision ==
                        candidate,
                ) {
                    "working carry rollover current readback differs from the committed revision"
                }

                CommitResult(
                    state = "COMMITTED",
                    current = rebound,
                )
            }
    }

    private fun validatePrepared(
        prepared: Prepared,
    ) {
        requireIdentity(
            companionLineageRef = prepared.companionLineageRef,
            threadRef = prepared.threadRef,
        )
        require(
            (
                prepared.priorRevisionRefOrNull ==
                    null
            ) ==
                (
                    prepared.priorRevisionFingerprintOrNull ==
                        null
                ),
        ) {
            "working carry prepared prior revision identity is partial"
        }
        prepared.priorRevisionRefOrNull
            ?.let {
                requireStableRef(
                    it,
                    "prepared.priorRevisionRefOrNull",
                )
            }
        prepared.priorRevisionFingerprintOrNull
            ?.let {
                requireSha256(
                    it,
                    "prepared.priorRevisionFingerprintOrNull",
                )
            }
        prepared.priorConversationHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "prepared.priorConversationHeadSha256",
                )
            }
        requireRequest(
            prepared.currentRequest,
        )
        val input =
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                prepared.refreshInput,
            )
        VexLifeWorkingCarryContract.requireDraftFromRefresh(
            refresh = input,
            draft = prepared.carry,
        )
        requireRefreshRequestMatches(
            refresh = input,
            currentRequest = prepared.currentRequest,
        )
        require(
            input.companionLineageRef ==
                prepared.companionLineageRef &&
                input.threadRef ==
                prepared.threadRef,
        ) {
            "prepared working carry refresh identity differs from its request"
        }
        requireFormationBinding(
            prepared.formationBinding,
        )
        require(
            prepared.formationBinding.refreshInputSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        input,
                    ),
                ) &&
                prepared.formationBinding.draftSha256 ==
                sha256(
                    VexContractJson.canonicalEncode(
                        prepared.carry,
                    ),
                ),
        ) {
            "prepared working carry formation binding is detached from its refresh input or Draft"
        }
    }

    private fun requirePriorStillCurrent(
        current: Current?,
        prepared: Prepared,
    ) {
        if (
            prepared.priorRevisionRefOrNull ==
                null
        ) {
            require(
                current ==
                    null,
            ) {
                "working carry currentness changed after prepare"
            }
            return
        }

        val exactCurrent =
            requireNotNull(
                current,
            ) {
                "working carry current revision disappeared after prepare"
            }
        require(
            exactCurrent.revision.revisionRef ==
                prepared.priorRevisionRefOrNull &&
                exactCurrent.revision.semanticFingerprint ==
                prepared.priorRevisionFingerprintOrNull,
        ) {
            "working carry current revision changed after prepare"
        }
    }

    private fun requireCompletedHead(
        prepared: Prepared,
        completedHead: DirectConversationStore.Head,
    ) {
        requireSha256(
            completedHead.conversationHeadSha256,
            "completedHead.conversationHeadSha256",
        )
        require(
            completedHead.companionLineageRef ==
                prepared.companionLineageRef &&
                completedHead.threadRef ==
                prepared.threadRef &&
                completedHead.turnRef ==
                prepared.currentRequest.turnRef &&
                completedHead.requestEventHash ==
                prepared.currentRequest.eventHash &&
                completedHead.sequence ==
                prepared.currentRequest.sequence +
                1 &&
                completedHead.priorConversationHeadSha256 ==
                prepared.priorConversationHeadSha256,
        ) {
            "working carry commit does not bind the exact successfully completed Conversation turn"
        }
    }

    private fun requireRefreshRequestMatches(
        refresh: VexLifeWorkingCarryContract.RefreshInput,
        currentRequest: DirectConversationStore.Event,
    ) {
        val source =
            refresh.currentRequest
        require(
            source.source.sequence ==
                currentRequest.sequence &&
                source.source.eventRef ==
                currentRequest.eventRef &&
                source.source.eventHash ==
                currentRequest.eventHash &&
                source.eventKind ==
                currentRequest.eventKind &&
                source.speakerRef ==
                currentRequest.speakerRef &&
                source.recipientRefs ==
                currentRequest.recipientRefs &&
                source.content ==
                currentRequest.content,
        ) {
            "working carry refresh current request differs from the exact durable request"
        }
    }

    private fun formRevision(
        prepared: Prepared,
        completedHead: DirectConversationStore.Head,
    ): Revision {
        val core =
            RevisionCore(
                schemaVersion = RevisionSchema,
                truthClass = "VEX_PRESENT_WORKING_CARRY_REVISION",
                ownerRef = OwnerRef,
                companionLineageRef = prepared.companionLineageRef,
                threadRef = prepared.threadRef,
                refreshMoment = VexLifeWorkingCarryContract.GradualAging,
                carry = prepared.carry,
                formationBinding = prepared.formationBinding,
                priorRevisionRefOrNull = prepared.priorRevisionRefOrNull,
                priorRevisionFingerprintOrNull = prepared.priorRevisionFingerprintOrNull,
                sourceConversationHeadSha256 = completedHead.conversationHeadSha256,
                sourceTurnRef = prepared.currentRequest.turnRef,
                sourceRequestEventRef = prepared.currentRequest.eventRef,
                sourceRequestEventHash = prepared.currentRequest.eventHash,
                sourceRequestSequence = prepared.currentRequest.sequence,
                sourceResponseEventHash = completedHead.eventHash,
                formedAt = completedHead.formedAt,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        return Revision(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            ownerRef = core.ownerRef,
            companionLineageRef = core.companionLineageRef,
            threadRef = core.threadRef,
            refreshMoment = core.refreshMoment,
            carry = core.carry,
            formationBinding = core.formationBinding,
            priorRevisionRefOrNull = core.priorRevisionRefOrNull,
            priorRevisionFingerprintOrNull = core.priorRevisionFingerprintOrNull,
            sourceConversationHeadSha256 = core.sourceConversationHeadSha256,
            sourceTurnRef = core.sourceTurnRef,
            sourceRequestEventRef = core.sourceRequestEventRef,
            sourceRequestEventHash = core.sourceRequestEventHash,
            sourceRequestSequence = core.sourceRequestSequence,
            sourceResponseEventHash = core.sourceResponseEventHash,
            formedAt = core.formedAt,
            revisionRef =
                "working-carry-revision." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
            .also(
                ::validateRevision,
            )
    }

    private fun requireRolloverRefreshMatchesCompletedHead(
        refresh: VexLifeWorkingCarryContract.RolloverRefreshInput,
        completedHead: DirectConversationStore.Head,
    ) {
        val request =
            refresh.handoffEvents[
                refresh.handoffEvents.size -
                    2
            ]
        val response =
            refresh.handoffEvents.last()

        require(
            request.eventKind ==
                "REQUEST" &&
                response.eventKind ==
                "RESPONSE" &&
                request.source.eventHash ==
                completedHead.requestEventHash &&
                request.source.sequence ==
                completedHead.sequence -
                    1 &&
                response.source.eventHash ==
                completedHead.eventHash &&
                response.source.sequence ==
                completedHead.sequence,
        ) {
            "working carry rollover handoff does not end at the exact completed Conversation pair"
        }
    }

    private fun formRolloverRevision(
        current: Current?,
        refresh: VexLifeWorkingCarryContract.RolloverRefreshInput,
        carry: VexLifeWorkingCarryContract.Draft,
        formationBinding: FormationBinding,
        completedHead: DirectConversationStore.Head,
    ): Revision {
        val request =
            refresh.handoffEvents[
                refresh.handoffEvents.size -
                    2
            ]
        val core =
            RevisionCore(
                schemaVersion = RevisionSchema,
                truthClass = "VEX_PRESENT_WORKING_CARRY_REVISION",
                ownerRef = OwnerRef,
                companionLineageRef = completedHead.companionLineageRef,
                threadRef = completedHead.threadRef,
                refreshMoment = VexLifeWorkingCarryContract.RolloverHandoff,
                carry = carry,
                formationBinding = formationBinding,
                priorRevisionRefOrNull =
                    current
                        ?.revision
                        ?.revisionRef,
                priorRevisionFingerprintOrNull =
                    current
                        ?.revision
                        ?.semanticFingerprint,
                sourceConversationHeadSha256 = completedHead.conversationHeadSha256,
                sourceTurnRef = completedHead.turnRef,
                sourceRequestEventRef = request.source.eventRef,
                sourceRequestEventHash = request.source.eventHash,
                sourceRequestSequence = request.source.sequence,
                sourceResponseEventHash = completedHead.eventHash,
                formedAt = completedHead.formedAt,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        return Revision(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            ownerRef = core.ownerRef,
            companionLineageRef = core.companionLineageRef,
            threadRef = core.threadRef,
            refreshMoment = core.refreshMoment,
            carry = core.carry,
            formationBinding = core.formationBinding,
            priorRevisionRefOrNull = core.priorRevisionRefOrNull,
            priorRevisionFingerprintOrNull = core.priorRevisionFingerprintOrNull,
            sourceConversationHeadSha256 = core.sourceConversationHeadSha256,
            sourceTurnRef = core.sourceTurnRef,
            sourceRequestEventRef = core.sourceRequestEventRef,
            sourceRequestEventHash = core.sourceRequestEventHash,
            sourceRequestSequence = core.sourceRequestSequence,
            sourceResponseEventHash = core.sourceResponseEventHash,
            formedAt = core.formedAt,
            revisionRef =
                "working-carry-revision." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
        )
            .also(
                ::validateRevision,
            )
    }

    private fun formHead(
        revision: Revision,
    ): Head {
        val core =
            HeadCore(
                schemaVersion = HeadSchema,
                truthClass = "CURRENT_VEX_PRESENT_WORKING_CARRY_HEAD",
                ownerRef = OwnerRef,
                companionLineageRef = revision.companionLineageRef,
                threadRef = revision.threadRef,
                revisionRef = revision.revisionRef,
                revisionFingerprint = revision.semanticFingerprint,
                committedAt = revision.formedAt,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        return Head(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            ownerRef = core.ownerRef,
            companionLineageRef = core.companionLineageRef,
            threadRef = core.threadRef,
            revisionRef = core.revisionRef,
            revisionFingerprint = core.revisionFingerprint,
            committedAt = core.committedAt,
            semanticFingerprint = fingerprint,
        )
            .also(
                ::validateHead,
            )
    }

    private fun validateRevision(
        value: Revision,
    ) {
        require(
            value.schemaVersion ==
                RevisionSchema &&
                value.truthClass ==
                "VEX_PRESENT_WORKING_CARRY_REVISION" &&
                value.ownerRef ==
                OwnerRef &&
                value.refreshMoment in
                setOf(
                    VexLifeWorkingCarryContract.GradualAging,
                    VexLifeWorkingCarryContract.RolloverHandoff,
                ) &&
                (
                    value.priorRevisionRefOrNull ==
                        null
                ) ==
                    (
                        value.priorRevisionFingerprintOrNull ==
                            null
                    ),
        ) {
            "working carry revision identity is invalid"
        }
        requireIdentity(
            companionLineageRef = value.companionLineageRef,
            threadRef = value.threadRef,
        )
        VexLifeWorkingCarryContract.requireDraftShape(
            value.carry,
        )
        requireFormationBinding(
            value.formationBinding,
        )
        require(
            value.carry.companionLineageRef ==
                value.companionLineageRef &&
                value.carry.threadRef ==
                value.threadRef,
        ) {
            "working carry revision carries a different lineage/thread"
        }
        value.priorRevisionRefOrNull
            ?.let {
                requireStableRef(
                    it,
                    "revision.priorRevisionRefOrNull",
                )
            }
        value.priorRevisionFingerprintOrNull
            ?.let {
                requireSha256(
                    it,
                    "revision.priorRevisionFingerprintOrNull",
                )
            }
        requireSha256(
            value.sourceConversationHeadSha256,
            "revision.sourceConversationHeadSha256",
        )
        requireStableRef(
            value.sourceTurnRef,
            "revision.sourceTurnRef",
        )
        requireStableRef(
            value.sourceRequestEventRef,
            "revision.sourceRequestEventRef",
        )
        requireSha256(
            value.sourceRequestEventHash,
            "revision.sourceRequestEventHash",
        )
        require(
            value.sourceRequestSequence >=
                0,
        ) {
            "working carry revision source request sequence is negative"
        }
        requireSha256(
            value.sourceResponseEventHash,
            "revision.sourceResponseEventHash",
        )
        requireCanonicalTimestamp(
            value.formedAt,
            "revision.formedAt",
        )

        val core =
            RevisionCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                ownerRef = value.ownerRef,
                companionLineageRef = value.companionLineageRef,
                threadRef = value.threadRef,
                refreshMoment = value.refreshMoment,
                carry = value.carry,
                formationBinding = value.formationBinding,
                priorRevisionRefOrNull = value.priorRevisionRefOrNull,
                priorRevisionFingerprintOrNull = value.priorRevisionFingerprintOrNull,
                sourceConversationHeadSha256 = value.sourceConversationHeadSha256,
                sourceTurnRef = value.sourceTurnRef,
                sourceRequestEventRef = value.sourceRequestEventRef,
                sourceRequestEventHash = value.sourceRequestEventHash,
                sourceRequestSequence = value.sourceRequestSequence,
                sourceResponseEventHash = value.sourceResponseEventHash,
                formedAt = value.formedAt,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        require(
            value.semanticFingerprint ==
                fingerprint &&
                value.revisionRef ==
                "working-carry-revision." +
                fingerprint.take(
                    32,
                ),
        ) {
            "working carry revision semantic identity is invalid"
        }
    }

    private fun validateHead(
        value: Head,
    ) {
        require(
            value.schemaVersion ==
                HeadSchema &&
                value.truthClass ==
                "CURRENT_VEX_PRESENT_WORKING_CARRY_HEAD" &&
                value.ownerRef ==
                OwnerRef,
        ) {
            "working carry head identity is invalid"
        }
        requireIdentity(
            companionLineageRef = value.companionLineageRef,
            threadRef = value.threadRef,
        )
        requireStableRef(
            value.revisionRef,
            "head.revisionRef",
        )
        requireSha256(
            value.revisionFingerprint,
            "head.revisionFingerprint",
        )
        requireCanonicalTimestamp(
            value.committedAt,
            "head.committedAt",
        )
        val core =
            HeadCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                ownerRef = value.ownerRef,
                companionLineageRef = value.companionLineageRef,
                threadRef = value.threadRef,
                revisionRef = value.revisionRef,
                revisionFingerprint = value.revisionFingerprint,
                committedAt = value.committedAt,
            )
        require(
            value.semanticFingerprint ==
                sha256(
                    VexContractJson.canonicalEncode(
                        core,
                    ),
                ),
        ) {
            "working carry head semantic identity is invalid"
        }
    }

    internal fun requireFormationBinding(
        value: FormationBinding,
    ): FormationBinding {
        require(
            value.schemaVersion ==
                FormationBindingSchema,
        ) {
            "working carry formation binding schema is invalid"
        }
        requireStableRef(
            value.formationRef,
            "formationBinding.formationRef",
        )
        requireSha256(
            value.formationFingerprint,
            "formationBinding.formationFingerprint",
        )
        requireSha256(
            value.refreshInputSha256,
            "formationBinding.refreshInputSha256",
        )
        requireSha256(
            value.draftSha256,
            "formationBinding.draftSha256",
        )
        require(
            value.formationRef ==
                "working-carry-formation." +
                value.formationFingerprint.take(
                    32,
                ),
        ) {
            "working carry formation ref does not bind its fingerprint"
        }
        requireStableRef(
            value.runtimeObservationRef,
            "formationBinding.runtimeObservationRef",
        )
        requireSha256(
            value.runtimeObservationFingerprint,
            "formationBinding.runtimeObservationFingerprint",
        )
        requireStableRef(
            value.invocationRef,
            "formationBinding.invocationRef",
        )
        requireSha256(
            value.invocationFingerprint,
            "formationBinding.invocationFingerprint",
        )
        requireSha256(
            value.providerOutputSha256,
            "formationBinding.providerOutputSha256",
        )
        return value
    }

    private fun requireRequest(
        value: DirectConversationStore.Event,
    ) {
        require(
            value.eventKind ==
                "REQUEST" &&
                value.sequence >=
                0 &&
                value.content.isNotEmpty(),
        ) {
            "working carry requires one exact durable REQUEST"
        }
        requireStableRef(
            value.eventRef,
            "currentRequest.eventRef",
        )
        requireSha256(
            value.eventHash,
            "currentRequest.eventHash",
        )
        requireStableRef(
            value.turnRef,
            "currentRequest.turnRef",
        )
    }

    private fun paths(
        companionLineageRef: String,
        threadRef: String,
    ): Paths {
        requireIdentity(
            companionLineageRef = companionLineageRef,
            threadRef = threadRef,
        )
        val key =
            sha256(
                companionLineageRef +
                    "|" +
                    threadRef,
            )
                .take(
                    32,
                )
        val root =
            VexOwnedPath.resolveInside(
                home,
                "working-carry/" +
                    key,
            )
        return Paths(
            root = root,
            revisions = root.resolve(
                "revisions",
            ),
            head = root.resolve(
                "head.json",
            ),
            lock = root.resolve(
                "owner.lock",
            ),
        )
    }

    private fun requireIdentity(
        companionLineageRef: String,
        threadRef: String,
    ) {
        require(
            companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "working carry owner identity is outside the current Product lineage/thread"
        }
    }

    private inline fun <reified T> writeImmutableOrRequireExact(
        path: Path,
        value: T,
    ) {
        val existing =
            VexDurableContractFile.read<T>(
                ownedRoot = home,
                path = path,
            )
        if (
            existing ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = home,
                path = path,
                value = value,
            )
        } else {
            require(
                existing ==
                    value,
            ) {
                "working carry immutable address already contains different source"
            }
        }
    }

    private fun requireCanonicalTimestamp(
        value: String,
        label: String,
    ) {
        val parsed =
            Instant.parse(
                value,
            )
        require(
            parsed.toString() ==
                value,
        ) {
            "$label must be canonical UTC"
        }
    }

    private fun requireStableRef(
        value: String,
        label: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <=
                512 &&
                value.none {
                    it.isWhitespace()
                },
        ) {
            "$label must be one non-empty stable ref"
        }
    }

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "$label must be one lowercase SHA-256"
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
