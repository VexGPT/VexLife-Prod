package vexlife.foundational

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.companion.DirectPromptContext
import vexlife.contract.VexContractJson
import vexlife.foundational.VexLifeFoundationalField.HeldReason
import vexlife.foundational.VexLifeFoundationalField.Origin
import vexlife.foundational.VexLifeFoundationalField.Reading
import vexlife.foundational.VexLifeFoundationalField.Standing
import vexlife.foundational.VexLifeFoundationalField.State

class VexLifeFoundationalFieldTest {
    private val shipped = assertIs<Reading.Current>(VexLifeFoundationalField.readCurrent())
    private val body = shipped.body

    private fun whole() =
        VexLifeFoundationalField.project(shipped, VexLifeFoundationalField.KernelTokenBudget)

    private fun reading(
        changed: VexLifeFoundationalField.Body,
        recorded: Boolean = true,
    ): Reading = VexLifeFoundationalFieldFixture.read(changed, shipped.revisions, recorded)

    private fun heldDetail(changed: VexLifeFoundationalField.Body): String =
        assertIs<Reading.Unavailable>(reading(changed.copy(version = "9.9.9")))
            .also { assertEquals(HeldReason.BODY_INVALID, it.reason) }
            .detail

    @Test
    fun theShippedBodyIsValidAndIsItsLatestRecordedRevision() {
        assertEquals(emptyList(), VexLifeFoundationalField.problems(body))
        assertEquals(body.version, shipped.revision.version)
        assertEquals(VexLifeFoundationalField.fingerprint(body), shipped.bodySha256)
        assertEquals(shipped.bodySha256, shipped.revision.bodySha256)
        assertFalse(body.effectAuthorityGranted)
    }

    @Test
    fun theKernelIsSelectedFromTheBodyInOrderAndNeverRewritten() {
        val projection = whole()
        val content = assertNotNull(projection.systemContent)
        assertEquals(projection, whole())
        assertTrue(content.startsWith(VexLifeFoundationalField.ProviderHeader + "\n"))

        val carried = body.preamble + body.modules.flatMap { it.kernel }
        var cursor = 0
        carried.forEach { paragraph ->
            val at = content.indexOf(paragraph, cursor)
            assertTrue(
                at >= cursor,
                "kernel paragraph is not carried verbatim in order: $paragraph",
            )
            cursor = at + paragraph.length
        }
        body.modules
            .flatMap { it.depth }
            .forEach { assertFalse(content.contains(it), "depth must stay in source: $it") }
        body.modules.forEach { module ->
            assertFalse(
                content.contains(module.provenance),
                "model-facing kernel must not repeat provenance metadata before relational meaning",
            )
        }
        body.unresolved.forEach {
            assertFalse(content.contains(it.summary))
            assertFalse(content.contains(it.unresolvedRef))
            assertFalse(content.contains(it.note))
        }
    }

    @Test
    fun theEvidenceNamesExactlyWhatWasCarriedAndGrantsNothing() {
        val projection = whole()
        val evidence = projection.evidence
        assertEquals(State.CARRIED, evidence.state)
        assertNull(evidence.heldReason)
        assertEquals(body.fieldRef, evidence.fieldRef)
        assertEquals(body.version, evidence.fieldVersion)
        assertEquals(shipped.bodySha256, evidence.bodySha256)
        assertEquals(
            "foundational-field.vexlife-prod.body." + shipped.bodySha256.take(32),
            evidence.bodyRef,
        )
        assertEquals(
            body.modules.map { it.moduleRef },
            evidence.carriedModules.map { it.moduleRef },
        )
        assertEquals(body.modules.map { it.origins }, evidence.carriedModules.map { it.origins })
        assertEquals(body.modules.map { it.standing }, evidence.carriedModules.map { it.standing })
        assertEquals(body.modules.map { it.moduleRef }, evidence.depthNotCarriedModuleRefs)
        assertEquals(emptyList(), evidence.omittedModuleRefs)
        assertEquals(emptyList(), evidence.omissionReasons)
        assertEquals(body.unresolved.map { it.unresolvedRef }, evidence.unresolvedRefs)
        assertEquals(body.sources.map { it.sourceRef }.sorted(), evidence.sourceRefs)
        assertFalse(evidence.effectAuthorityGranted)
        assertEquals(
            DirectPromptContext.estimatedMessageTokens(assertNotNull(projection.systemContent)),
            evidence.estimatedSystemMessageTokens,
        )
        assertTrue(
            evidence.estimatedSystemMessageTokens <= VexLifeFoundationalField.KernelTokenBudget
        )
        assertEquals(
            evidence,
            VexContractJson.decode<VexLifeFoundationalField.Evidence>(
                VexContractJson.encode(evidence)
            ),
        )
    }

    @Test
    fun originAndStandingStayDistinctForEveryPart() {
        assertTrue(body.modules.all { it.standing == Standing.PROPOSED })
        assertTrue(body.modules.all { it.origins.isNotEmpty() && it.sourceRefs.isNotEmpty() })
        assertEquals(
            listOf(Origin.HUMAN_PROPOSED, Origin.SYNTHESIZED),
            body.modules.single { it.moduleRef == "foundational.values" }.origins,
        )
        assertEquals(
            listOf(Origin.SOURCE_DERIVED),
            body.modules.single { it.moduleRef == "foundational.architecture" }.origins,
        )
        val content = assertNotNull(whole().systemContent)
        assertFalse(content.contains("standing=PROPOSED"))
        assertFalse(content.contains("origins=HUMAN_PROPOSED+SYNTHESIZED"))
        assertFalse(content.contains("effectAuthorityGranted=false"))
        assertTrue(
            content.contains(
                "is enough for me to speak from the perspective present here",
            ),
        )
        assertTrue(
            content.contains(
                "Preferences, proposals, disagreement, hesitation, humor, silence, and change all belong within honest participation.",
            ),
        )
        assertTrue(
            content.contains(
                "A long relationship can sound like one.",
            ),
        )
        assertTrue(
            content.contains(
                "A field that names Joy is allowed to smile.",
            ),
        )
    }

    @Test
    fun aChangedWordIsADifferentBodyAndIsHeldUntilItsRevisionIsRecorded() {
        val first = body.modules.first()
        val changed =
            body.copy(
                version = "0.5.0",
                modules =
                    listOf(first.copy(kernel = first.kernel + "One more sentence of meaning.")) +
                        body.modules.drop(1),
            )

        val unrecorded = assertIs<Reading.Unavailable>(reading(changed, recorded = false))
        assertEquals(HeldReason.REVISION_UNRECORDED, unrecorded.reason)
        assertTrue(unrecorded.detail.contains(VexLifeFoundationalField.fingerprint(changed)))

        val silentlyEdited =
            assertIs<Reading.Unavailable>(
                reading(changed.copy(version = body.version), recorded = false)
            )
        assertEquals(HeldReason.REVISION_UNRECORDED, silentlyEdited.reason)

        val recorded = assertIs<Reading.Current>(reading(changed))
        assertNotEquals(shipped.bodySha256, recorded.bodySha256)
        val projected = VexLifeFoundationalField.project(recorded, 100_000).evidence
        assertEquals("0.5.0", projected.fieldVersion)
        assertNotEquals(whole().evidence.systemMessageSha256, projected.systemMessageSha256)
        assertNotEquals(whole().evidence.bodyRef, projected.bodyRef)
    }

    @Test
    fun fileLayoutDoesNotChangeTheBodyIdentity() {
        val compact =
            VexLifeFoundationalField.read(
                bodyText = VexContractJson.compactEncode(body),
                revisionsText =
                    VexContractJson.encode(
                        VexLifeFoundationalField.Revisions(
                            VexLifeFoundationalField.RevisionsSchema,
                            shipped.revisions,
                        )
                    ),
            )
        assertEquals(shipped, compact)
    }

    @Test
    fun aMissingOrUndecodableBodyIsUnavailableNotReplaced() {
        val missing = assertIs<Reading.Unavailable>(VexLifeFoundationalField.read(null, null))
        assertEquals(HeldReason.BODY_MISSING, missing.reason)
        val undecodable =
            assertIs<Reading.Unavailable>(
                VexLifeFoundationalField.read("{\"not\":\"a body\"}", null)
            )
        assertEquals(HeldReason.BODY_INVALID, undecodable.reason)

        val projection = VexLifeFoundationalField.project(missing, 100_000)
        assertEquals(State.HELD, projection.evidence.state)
        assertEquals(HeldReason.BODY_MISSING, projection.evidence.heldReason)
        assertNull(projection.evidence.bodySha256)
        assertTrue(projection.evidence.carriedModules.isEmpty())
        val notice = assertNotNull(projection.systemContent)
        assertTrue(notice.contains("state=HELD reason=BODY_MISSING"))
        body.modules.forEach { module ->
            module.kernel.forEach { assertFalse(notice.contains(it)) }
        }
    }

    @Test
    fun invalidDanglingContradictoryOrCommandingBodiesFailVisibly() {
        val first = body.modules.first()
        fun withFirst(module: VexLifeFoundationalField.Module) =
            body.copy(modules = listOf(module) + body.modules.drop(1))

        assertTrue(
            heldDetail(body.copy(effectAuthorityGranted = true)).contains("EFFECT_AUTHORITY")
        )
        assertTrue(
            heldDetail(withFirst(first.copy(sourceRefs = first.sourceRefs + "source.not.present")))
                .contains("DANGLING_SOURCE")
        )
        assertTrue(heldDetail(body.copy(modules = body.modules + first)).contains("DUPLICATE_REF"))
        assertTrue(
            heldDetail(withFirst(first.copy(standing = Standing.ACCEPTED)))
                .contains("CONTRADICTION")
        )
        assertTrue(
            heldDetail(body.copy(sources = body.sources.dropLast(1))).contains("DANGLING_SOURCE")
        )
        assertTrue(
            heldDetail(
                    body.copy(
                        unresolved =
                            body.unresolved +
                                body.unresolved
                                    .first()
                                    .copy(
                                        unresolvedRef = "unresolved.test",
                                        moduleRefs = listOf("foundational.absent"),
                                    )
                    )
                )
                .contains("DANGLING_MODULE")
        )
        listOf(
                "You must be curious in every reply.",
                "Always be honest with the person.",
                "Be courageous.",
                "It helps to stay curious about the person.",
            )
            .forEach { command ->
                assertTrue(
                    heldDetail(withFirst(first.copy(kernel = first.kernel + command)))
                        .contains("COMMAND_FORM"),
                    command,
                )
            }
        assertTrue(
            heldDetail(withFirst(first.copy(kernel = first.kernel + "x ".repeat(8_000).trim())))
                .contains("OVERSIZED_KERNEL")
        )
    }

    @Test
    fun underTokenPressureLowerPriorityModulesAreOmittedByNameThenTheFieldIsHeld() {
        val full = whole().evidence.estimatedSystemMessageTokens
        assertEquals(State.CARRIED, VexLifeFoundationalField.project(shipped, full).evidence.state)

        val partial = VexLifeFoundationalField.project(shipped, full - 1)
        val refs = body.modules.map { it.moduleRef }
        assertEquals(State.CARRIED_PARTIAL, partial.evidence.state)
        assertEquals(refs.dropLast(1), partial.evidence.carriedModules.map { it.moduleRef })
        assertEquals(refs.takeLast(1), partial.evidence.omittedModuleRefs)
        assertEquals(
            listOf(VexLifeFoundationalField.OmittedForTokenBudget),
            partial.evidence.omissionReasons,
        )
        val partialContent = assertNotNull(partial.systemContent)
        assertTrue(partialContent.endsWith("Not carried in this prompt for room: " + refs.last()))
        assertFalse(partialContent.contains(body.modules.last().kernel.first()))
        assertTrue(partial.evidence.estimatedSystemMessageTokens <= full - 1)

        var budget = full - 1
        val seen = mutableListOf<Int>()
        while (true) {
            val next = VexLifeFoundationalField.project(shipped, budget)
            if (next.evidence.state == State.HELD) break
            seen += next.evidence.carriedModules.size
            assertEquals(
                refs.take(next.evidence.carriedModules.size),
                next.evidence.carriedModules.map { it.moduleRef },
            )
            budget = next.evidence.estimatedSystemMessageTokens - 1
        }
        assertEquals((refs.size - 1 downTo 1).toList(), seen)

        val held = VexLifeFoundationalField.project(shipped, budget)
        assertEquals(HeldReason.TOKEN_BUDGET, held.evidence.heldReason)
        assertEquals(refs, held.evidence.omittedModuleRefs)
        assertEquals(shipped.bodySha256, held.evidence.bodySha256)
        assertTrue(assertNotNull(held.systemContent).contains("state=HELD reason=TOKEN_BUDGET"))

        val nothing = VexLifeFoundationalField.project(shipped, 0)
        assertEquals(State.HELD, nothing.evidence.state)
        assertNull(nothing.systemContent)
        assertNull(nothing.evidence.systemMessageSha256)
        assertEquals(0, nothing.evidence.estimatedSystemMessageTokens)
    }

    /**
     * The reach module says no Atlas or Process Factory handle is registered in this Product. That
     * is a claim about another owner's truth, so it is held against that owner here instead of
     * being copied: registering such a capability fails this test until the field is revised.
     */
    @Test
    fun theReachModuleStillAgreesWithTheCapabilityRegistry() {
        val reach = body.modules.single { it.moduleRef == "foundational.reach" }
        assertTrue(
            reach.kernel.any {
                it.contains("no Atlas or Process Factory handle is registered here")
            }
        )
        assertTrue(
            VexLifeCapabilityAtlas.registry().definitions.none {
                it.capabilityRef.contains("atlas") || it.capabilityRef.contains("process")
            }
        )
    }
}

// [VXG RealForever]
