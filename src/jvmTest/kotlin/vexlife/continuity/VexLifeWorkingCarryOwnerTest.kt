package vexlife.continuity

import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vexlife.contract.VexContractJson
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeWorkingCarryOwnerTest {
    private fun formationBinding(
        refresh: VexLifeWorkingCarryContract.RefreshInput,
        carry: VexLifeWorkingCarryContract.Draft,
        hex: Char,
    ): VexLifeWorkingCarryOwner.FormationBinding {
        require(
            hex in
                "0123456789abcdef",
        )
        val fingerprint =
            hex
                .toString()
                .repeat(
                    64,
                )
        return VexLifeWorkingCarryOwner.FormationBinding(
            schemaVersion = VexLifeWorkingCarryOwner.FormationBindingSchema,
            formationRef =
                "working-carry-formation." +
                    fingerprint.take(
                        32,
                    ),
            formationFingerprint = fingerprint,
            refreshInputSha256 =
                sha256(
                    VexContractJson.canonicalEncode(
                        refresh,
                    ),
                ),
            draftSha256 =
                sha256(
                    VexContractJson.canonicalEncode(
                        carry,
                    ),
                ),
            runtimeObservationRef =
                "observation.working-carry.formation." +
                    hex,
            runtimeObservationFingerprint =
                "d".repeat(
                    64,
                ),
            invocationRef =
                "evidence.working-carry.formation." +
                    hex,
            invocationFingerprint =
                "e".repeat(
                    64,
                ),
            providerOutputSha256 =
                "f".repeat(
                    64,
                ),
        )
    }

    private fun rolloverFormationBinding(
        refresh: VexLifeWorkingCarryContract.RolloverRefreshInput,
        carry: VexLifeWorkingCarryContract.Draft,
        hex: Char,
    ): VexLifeWorkingCarryOwner.FormationBinding {
        require(
            hex in
                "0123456789abcdef",
        )
        val fingerprint =
            hex
                .toString()
                .repeat(
                    64,
                )
        return VexLifeWorkingCarryOwner.FormationBinding(
            schemaVersion = VexLifeWorkingCarryOwner.FormationBindingSchema,
            formationRef =
                "working-carry-formation." +
                    fingerprint.take(
                        32,
                    ),
            formationFingerprint = fingerprint,
            refreshInputSha256 =
                sha256(
                    VexContractJson.canonicalEncode(
                        refresh,
                    ),
                ),
            draftSha256 =
                sha256(
                    VexContractJson.canonicalEncode(
                        carry,
                    ),
                ),
            runtimeObservationRef =
                "observation.working-carry.rollover-formation." +
                    hex,
            runtimeObservationFingerprint =
                "d".repeat(
                    64,
                ),
            invocationRef =
                "evidence.working-carry.rollover-formation." +
                    hex,
            invocationFingerprint =
                "e".repeat(
                    64,
                ),
            providerOutputSha256 =
                "f".repeat(
                    64,
                ),
        )
    }

    private fun source(
        event: DirectConversationStore.Event,
    ) =
        VexLifeWorkingCarryContract.SourceEvent(
            source =
                VexLifeWorkingCarryContract.SourceBinding(
                    eventRef = event.eventRef,
                    eventHash = event.eventHash,
                    sequence = event.sequence,
                ),
            eventKind = event.eventKind,
            speakerRef = event.speakerRef,
            recipientRefs = event.recipientRefs,
            content = event.content,
        )

    private fun carry(
        lineageRef: String,
        threadRef: String,
        source: VexLifeWorkingCarryContract.SourceBinding,
        text: String =
            "I still owe this continuation.",
        carryTokenCap: Int =
            16_000,
    ): VexLifeWorkingCarryContract.Draft {
        val item =
            VexLifeWorkingCarryContract.CarryItem(
                kind = "OWED_CONTINUATION",
                content = text,
                sourceBindings =
                    listOf(
                        source,
                    ),
            )
        return VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = lineageRef,
            threadRef = threadRef,
            authoredByRef = "companion.vex.current",
            sourceBindings =
                listOf(
                    source,
                ),
            disposition = VexLifeWorkingCarryContract.Carry,
            carryItems =
                listOf(
                    item,
                ),
            releaseReason = null,
            carryTokenCap = carryTokenCap,
            estimatedCarryTokens =
                VexLifeWorkingCarryContract.estimateCarryContentTokens(
                    listOf(
                        item,
                    ),
                ),
        )
    }

    private fun release(
        lineageRef: String,
        threadRef: String,
        carryTokenCap: Int =
            0,
    ) =
        VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = lineageRef,
            threadRef = threadRef,
            authoredByRef = "companion.vex.current",
            sourceBindings = emptyList(),
            disposition = VexLifeWorkingCarryContract.Release,
            carryItems = emptyList(),
            releaseReason =
                VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
            carryTokenCap = carryTokenCap,
            estimatedCarryTokens = 0,
        )

    private fun refresh(
        lineageRef: String,
        threadRef: String,
        previousCarry: VexLifeWorkingCarryContract.Draft?,
        newlyLeaving: List<DirectConversationStore.Event>,
        currentRequest: DirectConversationStore.Event,
        carryTokenCap: Int,
    ) =
        VexLifeWorkingCarryContract.RefreshInput(
            schemaVersion = VexLifeWorkingCarryContract.RefreshInputSchema,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = lineageRef,
            threadRef = threadRef,
            previousCarry = previousCarry,
            newlyLeavingEvents =
                newlyLeaving.map(
                    ::source,
                ),
            retainedDirectEvents = emptyList(),
            currentRequest =
                source(
                    currentRequest,
                ),
            carryTokenCap = carryTokenCap,
        )

    private fun rolloverRefresh(
        lineageRef: String,
        threadRef: String,
        previousCarry: VexLifeWorkingCarryContract.Draft?,
        handoff: List<DirectConversationStore.Event>,
        carryTokenCap: Int,
    ) =
        VexLifeWorkingCarryContract.RolloverRefreshInput(
            schemaVersion =
                VexLifeWorkingCarryContract.RolloverRefreshInputSchema,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = lineageRef,
            threadRef = threadRef,
            previousCarry = previousCarry,
            handoffEvents =
                handoff.map(
                    ::source,
                ),
            carryTokenCap = carryTokenCap,
        )

    @Test
    fun preparedCarryBecomesCurrentOnlyAfterItsExactConversationTurnCompletes() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-working-carry-owner-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home = home,
                    deviceSuffix = "working-carry-owner-device",
                    lineageSuffix = "working-carry-owner-lineage",
                )
            val store =
                DirectConversationStore(
                    home,
                )
            val owner =
                VexLifeWorkingCarryOwner(
                    home,
                )

            var firstRequestOrNull: DirectConversationStore.Event? =
                null
            var firstResponseOrNull: DirectConversationStore.Event? =
                null
            var firstHeadOrNull: DirectConversationStore.Head? =
                null
            var secondRequestOrNull: DirectConversationStore.Event? =
                null
            var secondResponseOrNull: DirectConversationStore.Event? =
                null
            var secondHeadOrNull: DirectConversationStore.Head? =
                null
            var firstCarryRevisionRefOrNull: String? =
                null

            store.withWriterLease {
                val firstRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.001",
                        requestMessageRef = "message.working-carry.human.001",
                        content = "First request that may later leave direct presence.",
                        formedAt = Instant.parse(
                            "2026-11-10T20:00:00Z",
                        ),
                    )
                firstRequestOrNull =
                    firstRequest
                val firstResponse =
                    appendResponse(
                        request = firstRequest,
                        responseMessageRef = "message.working-carry.companion.001",
                        content = "First response with an unfinished continuation.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.runtime.001",
                        invocationEvidenceRef = "evidence.working-carry.invocation.001",
                        formedAt = Instant.parse(
                            "2026-11-10T20:00:01Z",
                        ),
                    )
                firstResponseOrNull =
                    firstResponse
                val firstContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = firstRequest,
                        response = firstResponse,
                        formedAt = Instant.parse(
                            "2026-11-10T20:00:02Z",
                        ),
                    )
                val firstHead =
                    commitCompletedTurn(
                        request = firstRequest,
                        response = firstResponse,
                        contextSha256 = firstContext.sha256,
                        formedAt = Instant.parse(
                            "2026-11-10T20:00:03Z",
                        ),
                    )
                firstHeadOrNull =
                    firstHead

                val secondRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.002",
                        requestMessageRef = "message.working-carry.human.002",
                        content = "Second request causes the older pair to leave direct presence.",
                        formedAt = Instant.parse(
                            "2026-11-10T20:01:00Z",
                        ),
                    )
                secondRequestOrNull =
                    secondRequest

                val firstRequestSource =
                    requireNotNull(
                        firstRequestOrNull,
                    )
                val firstResponseSource =
                    requireNotNull(
                        firstResponseOrNull,
                    )
                val carry =
                    carry(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = secondRequest.threadRef,
                        source =
                            VexLifeWorkingCarryContract.SourceBinding(
                                eventRef = firstRequestSource.eventRef,
                                eventHash = firstRequestSource.eventHash,
                                sequence = firstRequestSource.sequence,
                            ),
                    )
                val refresh =
                    refresh(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = secondRequest.threadRef,
                        previousCarry = null,
                        newlyLeaving =
                            listOf(
                                firstRequestSource,
                                firstResponseSource,
                            ),
                        currentRequest = secondRequest,
                        carryTokenCap = carry.carryTokenCap,
                    )
                val firstFormationBinding =
                    formationBinding(
                        refresh = refresh,
                        carry = carry,
                        hex = 'a',
                    )
                val prepared =
                    owner.prepare(
                        priorConversationHeadSha256 =
                            requireNotNull(
                                firstHeadOrNull,
                            )
                                .conversationHeadSha256,
                        currentRequest = secondRequest,
                        refreshInput = refresh,
                        carry = carry,
                        formationBinding = firstFormationBinding,
                    )

                val changedItem =
                    carry.carryItems
                        .single()
                        .copy(
                            content = "I still owe a differently worded continuation.",
                        )
                val changedCarry =
                    carry.copy(
                        carryItems =
                            listOf(
                                changedItem,
                            ),
                        estimatedCarryTokens =
                            VexLifeWorkingCarryContract.estimateCarryContentTokens(
                                listOf(
                                    changedItem,
                                ),
                            ),
                    )
                VexLifeWorkingCarryContract.requireDraftFromRefresh(
                    refresh = refresh,
                    draft = changedCarry,
                )
                assertFailsWith<IllegalArgumentException> {
                    owner.prepare(
                        priorConversationHeadSha256 =
                            requireNotNull(
                                firstHeadOrNull,
                            )
                                .conversationHeadSha256,
                        currentRequest = secondRequest,
                        refreshInput = refresh,
                        carry = changedCarry,
                        formationBinding = firstFormationBinding,
                    )
                }

                assertNull(
                    owner.readCurrent(),
                )

                val secondResponse =
                    appendResponse(
                        request = secondRequest,
                        responseMessageRef = "message.working-carry.companion.002",
                        content = "Second response formed with the refreshed carry.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.runtime.002",
                        invocationEvidenceRef = "evidence.working-carry.invocation.002",
                        formedAt = Instant.parse(
                            "2026-11-10T20:01:01Z",
                        ),
                    )
                secondResponseOrNull =
                    secondResponse
                val secondContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = secondRequest,
                        response = secondResponse,
                        formedAt = Instant.parse(
                            "2026-11-10T20:01:02Z",
                        ),
                    )
                val secondHead =
                    commitCompletedTurn(
                        request = secondRequest,
                        response = secondResponse,
                        contextSha256 = secondContext.sha256,
                        formedAt = Instant.parse(
                            "2026-11-10T20:01:03Z",
                        ),
                    )
                secondHeadOrNull =
                    secondHead

                val committed =
                    owner.commitCompletedTurn(
                        prepared = prepared,
                        completedHead = secondHead,
                    )
                assertEquals(
                    "COMMITTED",
                    committed.state,
                )
                assertEquals(
                    carry,
                    committed.current.revision.carry,
                )
                assertEquals(
                    VexLifeWorkingCarryContract.GradualAging,
                    committed.current.revision.refreshMoment,
                )
                firstCarryRevisionRefOrNull =
                    committed.current.revision.revisionRef

                val replay =
                    owner.commitCompletedTurn(
                        prepared = prepared,
                        completedHead = secondHead,
                    )
                assertEquals(
                    "IDEMPOTENT_CURRENT",
                    replay.state,
                )
                assertEquals(
                    committed.current,
                    replay.current,
                )

                val thirdRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.003",
                        requestMessageRef = "message.working-carry.human.003",
                        content = "Third request resolves what remained.",
                        formedAt = Instant.parse(
                            "2026-11-10T20:02:00Z",
                        ),
                    )
                val currentCarry =
                    requireNotNull(
                        owner.readCurrent(),
                    )
                        .revision
                        .carry
                val released =
                    release(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = thirdRequest.threadRef,
                    )
                val releaseRefresh =
                    refresh(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = thirdRequest.threadRef,
                        previousCarry = currentCarry,
                        newlyLeaving =
                            listOf(
                                requireNotNull(
                                    secondRequestOrNull,
                                ),
                                requireNotNull(
                                    secondResponseOrNull,
                                ),
                            ),
                        currentRequest = thirdRequest,
                        carryTokenCap = released.carryTokenCap,
                    )
                val releasePrepared =
                    owner.prepare(
                        priorConversationHeadSha256 =
                            requireNotNull(
                                secondHeadOrNull,
                            )
                                .conversationHeadSha256,
                        currentRequest = thirdRequest,
                        refreshInput = releaseRefresh,
                        carry = released,
                        formationBinding =
                            formationBinding(
                                refresh = releaseRefresh,
                                carry = released,
                                hex = 'b',
                            ),
                    )

                val thirdResponse =
                    appendResponse(
                        request = thirdRequest,
                        responseMessageRef = "message.working-carry.companion.003",
                        content = "Third response completes the unfinished carry.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.runtime.003",
                        invocationEvidenceRef = "evidence.working-carry.invocation.003",
                        formedAt = Instant.parse(
                            "2026-11-10T20:02:01Z",
                        ),
                    )
                val thirdContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = thirdRequest,
                        response = thirdResponse,
                        formedAt = Instant.parse(
                            "2026-11-10T20:02:02Z",
                        ),
                    )
                val thirdHead =
                    commitCompletedTurn(
                        request = thirdRequest,
                        response = thirdResponse,
                        contextSha256 = thirdContext.sha256,
                        formedAt = Instant.parse(
                            "2026-11-10T20:02:03Z",
                        ),
                    )
                val releasedCurrent =
                    owner.commitCompletedTurn(
                        prepared = releasePrepared,
                        completedHead = thirdHead,
                    )
                        .current

                assertEquals(
                    VexLifeWorkingCarryContract.Release,
                    releasedCurrent.revision.carry.disposition,
                )
                assertEquals(
                    firstCarryRevisionRefOrNull,
                    releasedCurrent.revision.priorRevisionRefOrNull,
                )

                val fourthRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.004",
                        requestMessageRef = "message.working-carry.human.004",
                        content = "Fourth request will fail before completion.",
                        formedAt = Instant.parse(
                            "2026-11-10T20:03:00Z",
                        ),
                    )
                val failedCarry =
                    carry(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = fourthRequest.threadRef,
                        source =
                            VexLifeWorkingCarryContract.SourceBinding(
                                eventRef = thirdRequest.eventRef,
                                eventHash = thirdRequest.eventHash,
                                sequence = thirdRequest.sequence,
                            ),
                        text = "This candidate must not become current without a completed turn.",
                    )
                val failedRefresh =
                    refresh(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = fourthRequest.threadRef,
                        previousCarry = releasedCurrent.revision.carry,
                        newlyLeaving =
                            listOf(
                                thirdRequest,
                                thirdResponse,
                            ),
                        currentRequest = fourthRequest,
                        carryTokenCap = failedCarry.carryTokenCap,
                    )
                val failedPrepared =
                    owner.prepare(
                        priorConversationHeadSha256 = thirdHead.conversationHeadSha256,
                        currentRequest = fourthRequest,
                        refreshInput = failedRefresh,
                        carry = failedCarry,
                        formationBinding =
                            formationBinding(
                                refresh = failedRefresh,
                                carry = failedCarry,
                                hex = 'c',
                            ),
                    )

                assertEquals(
                    releasedCurrent,
                    owner.readCurrent(),
                )
                assertFailsWith<IllegalArgumentException> {
                    owner.commitCompletedTurn(
                        prepared = failedPrepared,
                        completedHead = thirdHead,
                    )
                }
                assertEquals(
                    releasedCurrent,
                    owner.readCurrent(),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun completedRolloverHandoffCommitsOnlyAgainstItsExactCompletedHeadAndReplaysIdempotently() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-working-carry-rollover-owner-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home = home,
                    deviceSuffix = "working-carry-rollover-owner-device",
                    lineageSuffix = "working-carry-rollover-owner-lineage",
                )
            val store =
                DirectConversationStore(
                    home,
                )
            val owner =
                VexLifeWorkingCarryOwner(
                    home,
                )

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.working-carry.rollover.001",
                        requestMessageRef = "message.working-carry.rollover.human.001",
                        content = "Carry what is still alive after this rollover.",
                        formedAt = Instant.parse(
                            "2026-11-11T04:40:00Z",
                        ),
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.working-carry.rollover.companion.001",
                        content = "I still owe the distinction after the rollover.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.rollover.runtime.001",
                        invocationEvidenceRef = "evidence.working-carry.rollover.invocation.001",
                        formedAt = Instant.parse(
                            "2026-11-11T04:40:01Z",
                        ),
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                        formedAt = Instant.parse(
                            "2026-11-11T04:40:02Z",
                        ),
                    )
                val completedHead =
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                        formedAt = Instant.parse(
                            "2026-11-11T04:40:03Z",
                        ),
                    )

                val output =
                    carry(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = request.threadRef,
                        source =
                            VexLifeWorkingCarryContract.SourceBinding(
                                eventRef = response.eventRef,
                                eventHash = response.eventHash,
                                sequence = response.sequence,
                            ),
                        text = "I still owe the distinction after the rollover.",
                    )
                val refresh =
                    rolloverRefresh(
                        lineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = request.threadRef,
                        previousCarry = null,
                        handoff =
                            listOf(
                                request,
                                response,
                            ),
                        carryTokenCap = output.carryTokenCap,
                    )
                val binding =
                    rolloverFormationBinding(
                        refresh = refresh,
                        carry = output,
                        hex = 'd',
                    )

                val committed =
                    owner.commitCompletedRolloverHandoff(
                        completedHead = completedHead,
                        refreshInput = refresh,
                        carry = output,
                        formationBinding = binding,
                    )

                assertEquals(
                    "COMMITTED",
                    committed.state,
                )
                assertEquals(
                    VexLifeWorkingCarryContract.RolloverHandoff,
                    committed.current.revision.refreshMoment,
                )
                assertEquals(
                    completedHead.conversationHeadSha256,
                    committed.current.revision.sourceConversationHeadSha256,
                )

                val replay =
                    owner.commitCompletedRolloverHandoff(
                        completedHead = completedHead,
                        refreshInput = refresh,
                        carry = output,
                        formationBinding = binding,
                    )
                assertEquals(
                    "IDEMPOTENT_CURRENT",
                    replay.state,
                )
                assertEquals(
                    committed.current,
                    replay.current,
                )

                val mismatched =
                    refresh.copy(
                        handoffEvents =
                            listOf(
                                source(
                                    request,
                                ),
                                source(
                                    response,
                                )
                                    .copy(
                                        source =
                                            source(
                                                response,
                                            )
                                                .source
                                                .copy(
                                                    eventHash =
                                                        "0".repeat(
                                                            64,
                                                        ),
                                                ),
                                    ),
                            ),
                    )
                assertFailsWith<IllegalArgumentException> {
                    owner.commitCompletedRolloverHandoff(
                        completedHead = completedHead,
                        refreshInput = mismatched,
                        carry = output,
                        formationBinding = binding,
                    )
                }
                assertEquals(
                    committed.current,
                    owner.readCurrent(),
                )
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
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
