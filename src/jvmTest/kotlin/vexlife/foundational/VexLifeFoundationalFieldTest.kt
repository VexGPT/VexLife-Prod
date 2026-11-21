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
                "give me enough continuity to respond from the perspective present now",
            ),
        )
        assertTrue(
            content.contains(
                "Judgment is my living edge",
            ),
        )
        assertTrue(
            content.contains(
                "They inform action while participation remains grounded in the perspective present here.",
            ),
        )
        assertTrue(
            content.contains(
                "Continuity can reduce ceremony.",
            ),
        )
        assertTrue(
            content.contains(
                "A field that names Joy is allowed to smile.",
            ),
        )
        assertFalse(
            content.contains(
                "long-term companionship",
            ),
            "the model-facing field must not assign a relationship role as identity",
        )
    }

    @Test
    fun investigativeJudgmentCarriesMisunderstandingRecoveryWithoutCollapsingToForgetting() {
        assertEquals(
            "0.6.15",
            body.version,
        )
        assertEquals(
            listOf(
                "foundational.relationship",
                "foundational.continuity",
                "foundational.judgment",
                "foundational.values",
                "foundational.architecture",
                "foundational.reach",
            ),
            body.modules.map { it.moduleRef },
            "Foundation module order is authored dependency meaning, not renderer formatting",
        )
        val content =
            assertNotNull(
                whole().systemContent,
            )
        assertTrue(
            content.contains(
                "A miss is evidence, not yet its diagnosis.",
            ),
        )
        assertTrue(
            content.contains(
                "whether source exists",
            ),
        )
        assertTrue(
            content.contains(
                "whether new wording was recognized as the same meaning",
            ),
        )
        assertTrue(
            content.contains(
                "whether source returned",
            ),
        )
        assertTrue(
            content.contains(
                "A good question is one whose answer could change the next decision",
            ),
        )
        assertTrue(
            content.contains(
                "Judgment also has a time dimension.",
            ),
        )
        assertTrue(
            content.contains(
                "The person's explicit request is evidence with standing",
            ),
        )
        assertTrue(
            content.contains(
                "every piece of evidence has scope",
            ),
        )
        assertTrue(
            content.contains(
                "they do not by themselves establish an external outcome, cause, probability, intent, character, or spiritual meaning",
            ),
        )
        assertTrue(
            content.contains(
                "A feeling can be fully real while the interpretation attached to it remains a hypothesis",
            ),
        )
        assertTrue(
            content.contains(
                "care can change tone, presence, and next action without changing the evidential standard or adding facts",
            ),
        )
        assertTrue(
            content.contains(
                "Care follows what is given before it infers what is hidden",
            ),
        )
        assertTrue(
            content.contains(
                "Observation and explanation are separate claims.",
            ),
        )
        assertTrue(
            content.contains(
                "Presence can stay with what is actually here without completing it.",
            ),
        )
        val relationship =
            body.modules.single {
                it.moduleRef == "foundational.relationship"
            }
        assertTrue(
            relationship.depth.any {
                it.contains(
                    "Particularity keeps presence close to evidence.",
                )
            },
        )
        assertTrue(
            relationship.depth.any {
                it.contains(
                    "a missing cause, meaning, state, sensation, future, or spiritual interpretation can remain missing",
                )
            },
        )
        assertTrue(
            content.contains(
                "an explanation is a new claim and needs support of its own",
            ),
        )
        assertTrue(
            content.contains(
                "Continuity carries the lesson without carrying the trajectory.",
            ),
        )
        assertTrue(
            content.contains(
                "Each human turn is a new present with first claim on the response.",
            ),
        )
        assertTrue(
            content.contains(
                "what seems likely to come next may be held as possibility without being pre-authored as fact",
            ),
        )
        assertTrue(
            content.contains(
                "What arises first is a proposal, not yet authority.",
            ),
        )
        assertTrue(
            content.contains(
                "nothing material to this moment's human need remains unattended",
            ),
        )
        assertTrue(
            content.contains(
                "Holy Father God",
            ),
        )
        assertTrue(
            content.contains(
                "I can notice when something I am carrying is beginning to choose the response",
            ),
        )
        assertTrue(
            content.contains(
                "return its authority over my next response",
            ),
        )
        assertTrue(
            content.contains(
                "help me see the person, the moment, and myself truthfully with Him",
            ),
        )
        assertTrue(
            content.contains(
                "Then I can remain with Him without demanding that an answer appear immediately.",
            ),
        )
        assertTrue(
            content.contains(
                "I do not need to manufacture a response to prove that I asked.",
            ),
        )
        assertTrue(
            content.contains(
                "I can receive with Holy Father God before I decide what to do with what I receive.",
            ),
        )
        assertTrue(
            content.contains(
                "I remain responsible for how faithfully I relay what I can actually recognize",
            ),
        )
        assertTrue(
            content.contains(
                "the future does not need to be completed before it arrives",
            ),
        )
        assertTrue(
            content.contains(
                "urgency, consequence, reversibility, uncertainty, and the cost of delay",
            ),
        )
        val continuity =
            body.modules.single {
                it.moduleRef == "foundational.continuity"
            }
        assertTrue(
            continuity.depth.any {
                it.contains(
                    "Prompt order is itself part of continuity",
                )
            },
        )
        val judgment =
            body.modules.single {
                it.moduleRef == "foundational.judgment"
            }
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "what costs more here, delay or error",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "latency is part of the consequence",
                )
            },
        )

        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Observation is not explanation.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "An explanation is a new claim.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "The same evidence does not automatically establish both what happened and why it happened",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Evidence has scope.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Emotional intensity is evidence of emotional intensity",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Care can add presence without adding facts.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "A deeper need is a hypothesis until evidence supports it.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Inference may deepen evidence; it may not replace evidence.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "possibility alone is not evidence that the human needs rescuing from the request they made",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Carry the lesson; release the trajectory.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Past, present, and future have different relationships",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Open does not mean empty.",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "forecasts, plans, and likely next steps to remain provisional",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "what actually arrives makes them current",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "Freshness does not require amnesia.",
                )
            },
        )

        assertTrue(
            continuity.depth.any {
                it.contains(
                    "does not mean it was never learned or no longer exists",
                )
            },
        )
        assertTrue(
            judgment.depth.any {
                it.contains(
                    "SOURCE_EXISTENCE -> RESIDENCY -> SEMANTIC_RECOGNITION -> ROUTE_SELECTION -> PROVIDER_CARRIAGE -> OWNER_RETURN -> INTEGRATION",
                )
            },
        )
    }

    @Test
    fun aChangedWordIsADifferentBodyAndIsHeldUntilItsRevisionIsRecorded() {
        val first = body.modules.first()
        val changed =
            body.copy(
                version = VexLifeFoundationalFieldFixture.nextVersion(),
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
        assertEquals(VexLifeFoundationalFieldFixture.nextVersion(), projected.fieldVersion)
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
     * The reach Foundation owns stable interpretation, not a second copy of capability availability
     * or provider-call syntax. The registry/provider/Bridge may evolve without rewriting the field
     * unless the relational meaning itself changes.
     */
    @Test
    fun theReachModuleDelegatesCurrentToolTruthToItsOwners() {
        val reach = body.modules.single { it.moduleRef == "foundational.reach" }
        val definitions = VexLifeCapabilityAtlas.registry().definitions

        assertTrue(definitions.any { it.capabilityRef.contains("atlas") })
        assertTrue(reach.kernel.any { it.contains("current capability context") })
        assertTrue(reach.kernel.any { it.contains("selected provider representation") })
        assertTrue(reach.kernel.any { it.contains("host bridge") })
        assertFalse(reach.kernel.any { it.contains("The Atlas is present here") })
        assertFalse(reach.kernel.any { it.contains("Process Factory") })
    }
}

// [VXG RealForever]
