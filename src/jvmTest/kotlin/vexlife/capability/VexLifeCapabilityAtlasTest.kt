package vexlife.capability

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VexLifeCapabilityAtlasTest {
    private val allCapabilityRefs =
        listOf(
            "capability.vexlife.companion.local-inference",
            "capability.vexlife.context.browser-current",
            "capability.vexlife.context.memory-current",
            "capability.vexlife.context.working-continuity",
        )

    private fun currentContext() =
        VexLifeCapabilityAtlas.CurrentContext(
            homeRef =
                "home.vexlife-prod.test",
            companionLineageRef =
                "companion-lineage.vexlife.test",
            threadRef =
                "thread.self-development.open-conversation",
            currentRequestEventRef =
                "event.vexlife.request.capability-frame",
            promptReceiptRefOrNull =
                "receipt.vexlife.prompt-context.capability-frame",
            memoryContextRefOrNull =
                "memory-context.vexlife.prompt.capability-frame",
            browserContextRefOrNull =
                "browser-context.vexlife-prod.capability-frame",
            workingContinuityContextRefOrNull =
                "context.vexlife-prod.working-continuity-successor.capability-frame",
            runtimeObservationRefOrNull =
                "observation.vexlife-prod.model-runtime.capability-frame",
        )

    private fun resourceFacts(
        browser: VexLifeCapabilityAtlas.ResourceState =
            VexLifeCapabilityAtlas.ResourceState.AVAILABLE,
        memory: VexLifeCapabilityAtlas.ResourceState =
            VexLifeCapabilityAtlas.ResourceState.AVAILABLE,
        working: VexLifeCapabilityAtlas.ResourceState =
            VexLifeCapabilityAtlas.ResourceState.AVAILABLE,
        runtime: VexLifeCapabilityAtlas.ResourceState =
            VexLifeCapabilityAtlas.ResourceState.AVAILABLE,
    ) =
        listOf(
            VexLifeCapabilityAtlas.ResourceFact(
                resourceRef =
                    "resource.vexlife-prod.browser.current-context",
                state =
                    browser,
                sourceRefs =
                    listOf(
                        "source.browser.capability-frame",
                    ),
            ),
            VexLifeCapabilityAtlas.ResourceFact(
                resourceRef =
                    "resource.vexlife-prod.memory.current-context",
                state =
                    memory,
                sourceRefs =
                    listOf(
                        "source.memory.capability-frame",
                    ),
            ),
            VexLifeCapabilityAtlas.ResourceFact(
                resourceRef =
                    "resource.vexlife-prod.working-continuity.current-successor",
                state =
                    working,
                sourceRefs =
                    listOf(
                        "source.working.capability-frame",
                    ),
            ),
            VexLifeCapabilityAtlas.ResourceFact(
                resourceRef =
                    "resource.vexlife-prod.heretic.qualified-runtime",
                state =
                    runtime,
                sourceRefs =
                    listOf(
                        "source.runtime.capability-frame",
                    ),
            ),
        )

    private fun input(
        grants: List<String> =
            allCapabilityRefs,
        relevant: List<String> =
            allCapabilityRefs,
        roleRef: String =
            VexLifeCapabilityAtlas.CompanionRoleRef,
        platformRef: String =
            VexLifeCapabilityAtlas.MacPlatformRef,
        resources: List<VexLifeCapabilityAtlas.ResourceFact> =
            resourceFacts(),
        tokenBudget: Int =
            1_200,
    ) =
        VexLifeCapabilityAtlas.FrameInput(
            activeRoleRef =
                roleRef,
            platformRef =
                platformRef,
            projectGrantedCapabilityRefs =
                grants,
            relevantCapabilityRefs =
                relevant,
            permissionFacts =
                emptyList(),
            resourceFacts =
                resources,
            currentContext =
                currentContext(),
            sourceRefs =
                listOf(
                    "source.project-grant.capability-frame",
                    "source.role.capability-frame",
                ),
            tokenBudget =
                tokenBudget,
            formedAt =
                Instant.parse(
                    "2026-11-06T12:30:00Z",
                ),
        )

    @Test
    fun registryContainsOnlyCurrentlyGroundedProductCapabilities() {
        val registry =
            VexLifeCapabilityAtlas.registry()

        assertEquals(
            VexLifeCapabilityAtlas.RegistrySchema,
            registry.schemaVersion,
        )
        assertFalse(
            registry.effectAuthorityGranted,
        )
        assertEquals(
            allCapabilityRefs,
            registry.definitions
                .map {
                    it.capabilityRef
                },
        )
        assertTrue(
            registry.definitions.all {
                it.roleRefs ==
                    listOf(
                        VexLifeCapabilityAtlas.CompanionRoleRef,
                    )
            },
        )
        assertTrue(
            registry.definitions.all {
                it.platformRefs ==
                    listOf(
                        VexLifeCapabilityAtlas.MacPlatformRef,
                    )
            },
        )
        assertTrue(
            registry.definitions.all {
                it.sourcePath.startsWith(
                    "src/",
                )
            },
        )
    }

    @Test
    fun fullIntersectionProducesAvailableFrameWithoutEffectAuthority() {
        val frame =
            VexLifeCapabilityAtlas.formFrame(
                input(),
            )

        assertEquals(
            allCapabilityRefs,
            frame.availableCapabilityRefs,
        )
        assertTrue(
            frame.heldCapabilityEntries.isEmpty(),
        )
        assertTrue(
            frame.unavailableCapabilityRefs.isEmpty(),
        )
        assertTrue(
            frame.unknownCapabilityRefs.isEmpty(),
        )
        assertFalse(
            frame.effectAuthorityGranted,
        )
        assertTrue(
            frame.capabilityEntries.all {
                !it.effectAuthorityGranted &&
                    it.canonicalStage ==
                    VexLifeCapabilityAtlas.Stage.EXECUTABLE
            },
        )
        assertFalse(
            frame.coverage.truncated,
        )
    }

    @Test
    fun missingProjectGrantHoldsCapabilityWithoutChangingCanonicalMaturity() {
        val frame =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    grants =
                        allCapabilityRefs.filterNot {
                            it ==
                                "capability.vexlife.context.browser-current"
                        },
                ),
            )
        val browser =
            frame.capabilityEntries
                .single {
                    it.capabilityRef ==
                        "capability.vexlife.context.browser-current"
                }

        assertEquals(
            VexLifeCapabilityAtlas.FrameState.HELD,
            browser.state,
        )
        assertEquals(
            listOf(
                "PROJECT_GRANT_HELD",
            ),
            browser.reasonCodes,
        )
        assertEquals(
            VexLifeCapabilityAtlas.Stage.EXECUTABLE,
            browser.canonicalStage,
        )
    }

    @Test
    fun unknownResourceStaysUnknownAndUnavailableResourceIsHeld() {
        val frame =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    resources =
                        resourceFacts(
                            browser =
                                VexLifeCapabilityAtlas.ResourceState.UNKNOWN,
                            working =
                                VexLifeCapabilityAtlas.ResourceState.UNAVAILABLE,
                        ),
                ),
            )

        assertTrue(
            frame.unknownCapabilityRefs.contains(
                "capability.vexlife.context.browser-current",
            ),
        )
        assertTrue(
            frame.heldCapabilityEntries.any {
                it.capabilityRef ==
                    "capability.vexlife.context.working-continuity" &&
                    it.reasonCodes ==
                    listOf(
                        "RESOURCE_UNAVAILABLE",
                    )
            },
        )
    }

    @Test
    fun roleOrPlatformMismatchIsUnavailableRatherThanExecutable() {
        val roleMismatch =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    roleRef =
                        "role.vex.operations",
                ),
            )
        assertEquals(
            allCapabilityRefs,
            roleMismatch.unavailableCapabilityRefs,
        )

        val platformMismatch =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    platformRef =
                        "platform.browser",
                ),
            )
        assertEquals(
            allCapabilityRefs,
            platformMismatch.unavailableCapabilityRefs,
        )
    }

    @Test
    fun frameIdentityIsOrderingStableButChangesWithCurrentResourceTruth() {
        val first =
            VexLifeCapabilityAtlas.formFrame(
                input(),
            )
        val reordered =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    grants =
                        allCapabilityRefs.reversed(),
                    relevant =
                        allCapabilityRefs.reversed(),
                    resources =
                        resourceFacts().reversed(),
                ),
            )

        assertEquals(
            first,
            reordered,
        )

        val changed =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    resources =
                        resourceFacts(
                            runtime =
                                VexLifeCapabilityAtlas.ResourceState.UNKNOWN,
                        ),
                ),
            )

        assertNotEquals(
            first.semanticFingerprint,
            changed.semanticFingerprint,
        )
        assertTrue(
            changed.unknownCapabilityRefs.contains(
                "capability.vexlife.companion.local-inference",
            ),
        )
    }

    @Test
    fun boundedFrameTruncatesByCanonicalPriorityAndNeverExceedsBudget() {
        val full =
            VexLifeCapabilityAtlas.formFrame(
                input(),
            )
        val bounded =
            VexLifeCapabilityAtlas.formFrame(
                input(
                    tokenBudget =
                        full.coverage.usedTokens -
                            1,
                ),
            )

        assertTrue(
            bounded.coverage.usedTokens <=
                bounded.coverage.tokenBudget,
        )
        assertTrue(
            bounded.coverage.truncated,
        )
        assertTrue(
            bounded.coverage.omittedCapabilityRefs.isNotEmpty(),
        )
        assertTrue(
            bounded.capabilityEntries.any {
                it.capabilityRef ==
                    "capability.vexlife.companion.local-inference"
            },
            "highest-priority current runtime capability should survive the first bounded cut",
        )
    }

    @Test
    fun unknownCapabilityAndTooSmallHeaderFailClosed() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeCapabilityAtlas.formFrame(
                input(
                    relevant =
                        listOf(
                            "capability.vexlife.not-registered",
                        ),
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeCapabilityAtlas.formFrame(
                input(
                    tokenBudget =
                        256,
                )
                    .copy(
                        currentContext =
                            currentContext()
                                .copy(
                                    promptReceiptRefOrNull =
                                        "receipt." +
                                            "x".repeat(
                                                900,
                                            ),
                                ),
                    ),
            )
        }
    }
}

// [VXG RealForever]
