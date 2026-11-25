package vexlife.capability

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceEffectsFamily

class VexLifeWorkspaceBridgeTest {
    private val sourceRef =
        VexLifeLocalSourcesFamily.sourceRefForSha256(
            "a".repeat(
                64,
            ),
        )

    private val grantRef =
        VexLifeWorkspaceEffectsFamily.GrantRefPrefix +
            "b".repeat(
                64,
            )

    private companion object {
        const val PrivateWorkspaceBody =
            "PRIVATE-WORKSPACE-BODY-DO-NOT-RETURN"
    }

    private fun parsed(
        content: String = PrivateWorkspaceBody,
    ): VexLifeCapabilityRequest.Parsed =
        requireNotNull(
            VexLifeCapabilityRequest.parseOrNull(
                """
                VEXLIFE_CAPABILITY_REQUEST/v1
                {"capability":"workspace","action":"write","arguments":{"source":"$sourceRef","path":"hello.txt","content":"$content"}}
                """.trimIndent(),
            ),
        )

    private fun receipt(
        operationRef: String,
        disposition: String,
    ) =
        VexLifeWorkspaceWrite.Receipt(
            schemaVersion =
                VexLifeWorkspaceWrite.ReceiptSchema,
            receiptRef =
                VexLifeWorkspaceWrite.ReceiptRefPrefix +
                    "c".repeat(
                        64,
                    ),
            intentRef =
                VexLifeWorkspaceWrite.IntentRefPrefix +
                    "d".repeat(
                        64,
                    ),
            operationRef =
                operationRef,
            grantRef =
                grantRef,
            sourceRef =
                sourceRef,
            relativePath =
                "hello.txt",
            priorSha256 =
                null,
            newSha256 =
                "e".repeat(
                    64,
                ),
            bytes =
                6,
            disposition =
                disposition,
            completedAt =
                "2026-11-25T03:20:00Z",
            externalEffectsExecuted =
                true,
            effectAuthorityGranted =
                true,
        )

    private fun admission(
        available: Set<String> =
            setOf(
                VexLifeWorkspaceEffectDefinitions.WriteRef,
            ),
    ) =
        VexLifeCapabilityBridge.Admission(
            productHome =
                Files.createTempDirectory(
                    "vexlife-workspace-bridge-",
                ),
            availableCapabilityRefs =
                available,
            requestNumber =
                1,
            tokensAvailable =
                10_000,
            requestEventHash =
                "f".repeat(
                    64,
                ),
        )

    private fun perform(
        outcome: (
            operationRef: String,
        ) -> VexLifeWorkspaceWrite.Outcome,
    ): VexLifeCapabilityBridge.Exchange {
        var observedOperationRef: String? =
            null
        val exchange =
            VexLifeCapabilityBridge.perform(
                parsed =
                    parsed(),
                admission =
                    admission(),
                ownerBindings =
                    VexLifeCapabilityBridge.OwnerBindings(
                        writeWorkspace = {
                            _,
                            operationRef,
                            observedSourceRef,
                            relativePath,
                            writtenContent,
                            expectedPriorSha256,
                            ->
                            assertEquals(
                                sourceRef,
                                observedSourceRef,
                            )
                            assertEquals(
                                "hello.txt",
                                relativePath,
                            )
                            assertEquals(
                                PrivateWorkspaceBody,
                                writtenContent,
                            )
                            assertEquals(
                                null,
                                expectedPriorSha256,
                            )
                            observedOperationRef =
                                operationRef
                            outcome(
                                operationRef,
                            )
                        },
                    ),
                estimateTokens = {
                    value ->
                    maxOf(
                        1,
                        (
                            value.length +
                                3
                        ) /
                            4,
                    )
                },
            )
        assertNotNull(
            observedOperationRef,
        )
        return exchange
    }

    @Test
    fun verifiedFreshWriteReportsEffectNowWithoutReturningPrivateContent() {
        val exchange =
            perform {
                operationRef ->
                VexLifeWorkspaceWrite.Outcome.Completed(
                    receipt =
                        receipt(
                            operationRef,
                            VexLifeWorkspaceWrite.DispositionCreated,
                        ),
                    effectPerformedNow =
                        true,
                    currentEffectAuthority =
                        true,
                )
            }

        assertEquals(
            VexLifeCapabilityBridge.ResultState.RETURNED,
            exchange.entry.state,
        )
        assertTrue(
            exchange.entry.externalEffectsExecuted,
        )
        assertTrue(
            exchange.entry.effectAuthorityGranted,
        )
        assertFalse(
            exchange.entry.externalEffectMayHaveExecuted,
        )
        assertFalse(
            exchange.resultMessage.content.contains(
                PrivateWorkspaceBody,
            ),
        )
        assertTrue(
            exchange.resultMessage.content.contains(
                VexLifeWorkspaceWrite.DispositionCreated,
            ),
        )

        val trace =
            VexLifeCapabilityBridge.trace(
                listOf(
                    exchange.entry,
                ),
            )
        assertTrue(
            trace.effectAuthorityGranted,
        )
        assertFalse(
            trace.rawResultContentPersisted,
        )
    }

    @Test
    fun recoveredReceiptReportsNoNewEffectAndNoRenewedAuthority() {
        val exchange =
            perform {
                operationRef ->
                VexLifeWorkspaceWrite.Outcome.Completed(
                    receipt =
                        receipt(
                            operationRef,
                            VexLifeWorkspaceWrite.DispositionRecovered,
                        ),
                    effectPerformedNow =
                        false,
                    currentEffectAuthority =
                        false,
                )
            }

        assertEquals(
            VexLifeCapabilityBridge.ResultState.RETURNED,
            exchange.entry.state,
        )
        assertFalse(
            exchange.entry.externalEffectsExecuted,
        )
        assertFalse(
            exchange.entry.effectAuthorityGranted,
        )
        assertFalse(
            exchange.entry.externalEffectMayHaveExecuted,
        )
        assertTrue(
            exchange.resultMessage.content.contains(
                "recovered without another write",
            ),
        )
        assertTrue(
            exchange.resultMessage.content.contains(
                "\"effectPerformedNow\":\"false\"",
            ),
        )
        assertTrue(
            exchange.resultMessage.content.contains(
                "\"currentEffectAuthority\":\"false\"",
            ),
        )

        assertFalse(
            VexLifeCapabilityBridge.trace(
                listOf(
                    exchange.entry,
                ),
            )
                .effectAuthorityGranted,
        )
    }

    @Test
    fun uncertainAttemptStaysUncertainAndRequiresSameIntentRecovery() {
        val exchange =
            perform {
                _ ->
                VexLifeWorkspaceWrite.Outcome.UnknownAfterAttempt(
                    intentRef =
                        VexLifeWorkspaceWrite.IntentRefPrefix +
                            "d".repeat(
                                64,
                            ),
                    reason =
                        "receipt persistence interrupted",
                    observedSha256 =
                        null,
                )
            }

        assertEquals(
            VexLifeCapabilityBridge.ResultState.FAILED,
            exchange.entry.state,
        )
        assertFalse(
            exchange.entry.externalEffectsExecuted,
        )
        assertFalse(
            exchange.entry.effectAuthorityGranted,
        )
        assertTrue(
            exchange.entry.externalEffectMayHaveExecuted,
        )
        assertTrue(
            exchange.resultMessage.content.contains(
                "same exact workspace write",
            ),
        )
    }

    @Test
    fun unavailableFrameRefusesBeforeTheEffectOwnerRuns() {
        var ownerCalled =
            false
        val exchange =
            VexLifeCapabilityBridge.perform(
                parsed =
                    parsed(),
                admission =
                    admission(
                        emptySet(),
                    ),
                ownerBindings =
                    VexLifeCapabilityBridge.OwnerBindings(
                        writeWorkspace = {
                            _,
                            _,
                            _,
                            _,
                            _,
                            _,
                            ->
                            ownerCalled =
                                true
                            error(
                                "owner must not run",
                            )
                        },
                    ),
                estimateTokens = {
                    value ->
                    maxOf(
                        1,
                        (
                            value.length +
                                3
                        ) /
                            4,
                    )
                },
            )

        assertEquals(
            VexLifeCapabilityBridge.ResultState.NOT_AVAILABLE,
            exchange.entry.state,
        )
        assertFalse(
            ownerCalled,
        )
        assertFalse(
            exchange.entry.externalEffectsExecuted,
        )
    }
}

// [VXG RealForever]
