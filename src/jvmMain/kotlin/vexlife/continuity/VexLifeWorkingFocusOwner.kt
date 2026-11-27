package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath

/**
 * Durable currentness owner for one lineage/thread Working Focus.
 *
 * This owner persists only bounded declarative focus plus exact source/currentness references. It
 * does not read source bodies, mutate Conversation/Memory, materialize prompts, rank Attention,
 * call a model, or own cache state. Immutable revisions and state transitions are written before
 * head.json; the head is the only currentness commit point.
 */
class VexLifeWorkingFocusOwner(productHome: Path) {
    companion object {
        const val RevisionSchema = "vexlife-prod.working-focus-revision/v1"
        const val FormationSchema = "vexlife-prod.working-focus-formation/v1"
        const val StateSchema = "vexlife-prod.working-focus-state/v1"
        const val HeadSchema = "vexlife-prod.working-focus-head/v1"
        const val EffectOwnerRef = VexLifeWorkingFocus.EffectOwnerRef
    }

    @Serializable
    enum class FocusState { ACTIVE, DISPLACED, CLEARED }

    @Serializable
    enum class Transition { SET_CURRENT, DISPLACE, RESUME, CLEAR }

    @Serializable
    data class FocusRevision(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val workingFocusRef: String,
        val formationRef: String,
        val subjectScopeRefs: List<String>,
        val objective: String,
        val currentQuestionOrNull: String?,
        val retainedRelationships: List<VexLifeWorkingFocus.Relationship>,
        val openEdges: List<VexLifeWorkingFocus.OpenEdge>,
        val nextProbeOrNull: String?,
        val sourceBindingRefs: List<String>,
        val sourceRefs: List<String>,
        val priorRevisionFingerprintOrNull: String?,
        val semanticFingerprint: String,
    )

    /**
     * One immutable consumption of a stable formation identity.
     *
     * This is not a second truth store. It prevents a previously consumed formationRef from
     * resurrecting a displaced/cleared focus or being rebound to different bytes.
     */
    @Serializable
    data class FormationBinding(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val formationRef: String,
        val workingFocusRef: String,
        val revisionFingerprint: String,
        val semanticFingerprint: String,
    )

    /** DISPLACED is itself the custody record; RESUME retains release + revalidation evidence. */
    @Serializable
    data class StateRecord(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val state: FocusState,
        val transition: Transition,
        val workingFocusRefOrNull: String?,
        val revisionFingerprintOrNull: String?,
        val priorStateFingerprintOrNull: String?,
        val causeOwnerRef: String,
        val causeRef: String,
        val causeBindingRefs: List<String>,
        val revalidatedSourceBindingRefs: List<String>,
        val formedAt: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class Head(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val stateFingerprint: String,
        val formedAt: String,
    )

    data class Current(
        val state: StateRecord,
        val revisionOrNull: FocusRevision?,
        val head: Head,
        val stateRef: String,
        val revisionRefOrNull: String?,
    )

    data class MutationResult(
        val state: String,
        val effectPerformedNow: Boolean,
        val current: Current,
    )

    @Serializable
    private data class RevisionPayload(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val workingFocusRef: String,
        val formationRef: String,
        val subjectScopeRefs: List<String>,
        val objective: String,
        val currentQuestionOrNull: String?,
        val retainedRelationships: List<VexLifeWorkingFocus.Relationship>,
        val openEdges: List<VexLifeWorkingFocus.OpenEdge>,
        val nextProbeOrNull: String?,
        val sourceBindingRefs: List<String>,
        val sourceRefs: List<String>,
        val priorRevisionFingerprintOrNull: String?,
    )

    @Serializable
    private data class FormationPayload(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val formationRef: String,
        val workingFocusRef: String,
        val revisionFingerprint: String,
    )

    @Serializable
    private data class StatePayload(
        val schemaVersion: String,
        val truthClass: String,
        val effectOwnerRef: String,
        val lineageRef: String,
        val threadRef: String,
        val state: FocusState,
        val transition: Transition,
        val workingFocusRefOrNull: String?,
        val revisionFingerprintOrNull: String?,
        val priorStateFingerprintOrNull: String?,
        val causeOwnerRef: String,
        val causeRef: String,
        val causeBindingRefs: List<String>,
        val revalidatedSourceBindingRefs: List<String>,
        val formedAt: String,
    )

    private data class Paths(
        val root: Path,
        val formations: Path,
        val revisions: Path,
        val states: Path,
        val head: Path,
        val lock: Path,
    )

    private val home = VexOwnedPath.requireRoot(productHome, "canonical VexLife Home")

    fun readCurrent(lineageRef: String, threadRef: String): Current? {
        requireRef(lineageRef, "lineageRef")
        requireRef(threadRef, "threadRef")
        val paths = paths(lineageRef, threadRef)
        if (VexOwnedPath.observeDirectory(home, paths.root) == VexOwnedPath.DirectoryState.Missing) {
            return null
        }
        val head = VexDurableContractFile.read<Head>(home, paths.head) ?: return null
        require(
            head.schemaVersion == HeadSchema &&
                head.truthClass == "CURRENT_WORKING_FOCUS_HEAD" &&
                head.effectOwnerRef == EffectOwnerRef &&
                head.lineageRef == lineageRef &&
                head.threadRef == threadRef,
        ) { "working focus head identity is invalid" }
        requireSha256(head.stateFingerprint, "head stateFingerprint")
        Instant.parse(head.formedAt)

        val state = requireNotNull(
            VexDurableContractFile.read<StateRecord>(
                home,
                paths.states.resolve(head.stateFingerprint + ".json"),
            ),
        ) { "working focus current state is missing" }
        validateState(state)
        require(
            state.semanticFingerprint == head.stateFingerprint &&
                state.lineageRef == lineageRef && state.threadRef == threadRef,
        ) { "working focus head/state binding is invalid" }

        val revision = state.revisionFingerprintOrNull?.let { fingerprint ->
            requireNotNull(
                VexDurableContractFile.read<FocusRevision>(
                    home,
                    paths.revisions.resolve(fingerprint + ".json"),
                ),
            ) { "working focus current revision is missing" }.also(::validateRevision)
        }
        require(
            if (state.state == FocusState.CLEARED) revision == null
            else revision != null &&
                revision.semanticFingerprint == state.revisionFingerprintOrNull &&
                revision.workingFocusRef == state.workingFocusRefOrNull &&
                revision.lineageRef == lineageRef && revision.threadRef == threadRef,
        ) { "working focus state/revision binding is invalid" }
        revision?.let {
            val formation = requireNotNull(readFormation(paths, it.formationRef)) {
                "working focus current revision has no immutable formation binding"
            }
            validateFormation(formation)
            require(
                formation.lineageRef == lineageRef &&
                    formation.threadRef == threadRef &&
                    formation.workingFocusRef == it.workingFocusRef &&
                    formation.revisionFingerprint == it.semanticFingerprint,
            ) {
                "working focus formation/revision binding is invalid"
            }
            if (state.transition == Transition.SET_CURRENT) {
                require(state.causeRef == it.formationRef && state.causeBindingRefs == it.sourceBindingRefs) {
                    "SET_CURRENT does not bind exact focus source currentness"
                }
            }
            if (state.transition == Transition.RESUME) {
                require(state.revalidatedSourceBindingRefs == it.sourceBindingRefs) {
                    "RESUME does not bind exact revalidated source currentness"
                }
            }
        }
        return Current(
            state = state,
            revisionOrNull = revision,
            head = head,
            stateRef = stateRef(state.semanticFingerprint),
            revisionRefOrNull = revision?.let { revisionRef(it.semanticFingerprint) },
        )
    }

    fun setCurrent(
        draft: VexLifeWorkingFocus.Draft,
        formedAt: Instant = Instant.now(),
    ): MutationResult {
        val normalized = VexLifeWorkingFocus.normalize(draft)
        val before = readCurrent(normalized.lineageRef, normalized.threadRef)
        before?.revisionOrNull?.let { existing ->
            if (before.state.state == FocusState.ACTIVE && existing.formationRef == normalized.formationRef) {
                require(revisionMatchesDraft(existing, normalized)) {
                    "working focus formationRef is already current with different truth"
                }
                return MutationResult("IDEMPOTENT_CURRENT", false, before)
            }
        }
        require(
            readFormation(
                paths(normalized.lineageRef, normalized.threadRef),
                normalized.formationRef,
            ) == null,
        ) {
            "working focus formationRef was already consumed; form fresh current truth with a fresh formationRef"
        }

        val prior = normalized.priorWorkingFocusRefOrNull?.let { priorRef ->
            requireNotNull(before) { "working focus update requires current focus" }
            require(before.state.state == FocusState.ACTIVE) {
                "working focus update requires exact current ACTIVE focus"
            }
            requireNotNull(before.revisionOrNull).also {
                require(it.workingFocusRef == priorRef) {
                    "working focus update prior ref does not match current focus"
                }
            }
        }
        val focusRef = prior?.workingFocusRef ?: focusRef(normalized)
        val revision = formRevision(normalized, focusRef, prior)
        val formation = formFormationBinding(revision)
        val state = formState(
            lineageRef = normalized.lineageRef,
            threadRef = normalized.threadRef,
            state = FocusState.ACTIVE,
            transition = Transition.SET_CURRENT,
            revision = revision,
            prior = before,
            causeOwnerRef = EffectOwnerRef,
            causeRef = normalized.formationRef,
            causeBindingRefs = normalized.sourceBindingRefs,
            revalidatedSourceBindingRefs = emptyList(),
            formedAt = formedAt,
        )
        return commit(before, revision, state, "ACTIVE_COMMITTED", formation)
    }

    fun displace(
        lineageRef: String,
        threadRef: String,
        causeOwnerRef: String,
        causeRef: String,
        causeBindingRefs: List<String>,
        formedAt: Instant = Instant.now(),
    ): MutationResult {
        requireRef(causeOwnerRef, "causeOwnerRef")
        requireRef(causeRef, "causeRef")
        val bindings = normalizedRefs(causeBindingRefs, "causeBindingRefs")
        val before = requireNotNull(readCurrent(lineageRef, threadRef)) {
            "working focus displacement requires current ACTIVE focus"
        }
        if (before.state.state == FocusState.DISPLACED) {
            if (before.state.causeOwnerRef == causeOwnerRef && before.state.causeRef == causeRef &&
                before.state.causeBindingRefs == bindings
            ) return MutationResult("IDEMPOTENT_DISPLACED", false, before)
            throw IllegalArgumentException("working focus v1 does not admit nested displacement")
        }
        require(before.state.state == FocusState.ACTIVE) {
            "working focus displacement requires current ACTIVE focus"
        }
        val revision = requireNotNull(before.revisionOrNull)
        val state = formState(
            lineageRef, threadRef, FocusState.DISPLACED, Transition.DISPLACE, revision, before,
            causeOwnerRef, causeRef, bindings, emptyList(), formedAt,
        )
        return commit(before, revision, state, "DISPLACED_COMMITTED")
    }

    fun resume(
        lineageRef: String,
        threadRef: String,
        displacedStateRef: String,
        releaseOwnerRef: String,
        releaseRef: String,
        releaseBindingRefs: List<String>,
        revalidatedSourceBindingRefs: List<String>,
        formedAt: Instant = Instant.now(),
    ): MutationResult {
        requireRef(displacedStateRef, "displacedStateRef")
        requireRef(releaseOwnerRef, "releaseOwnerRef")
        requireRef(releaseRef, "releaseRef")
        val releaseBindings = normalizedRefs(releaseBindingRefs, "releaseBindingRefs")
        val revalidated = normalizedRefs(revalidatedSourceBindingRefs, "revalidatedSourceBindingRefs")
        val before = requireNotNull(readCurrent(lineageRef, threadRef)) {
            "working focus resume requires current displacement"
        }
        if (before.state.state == FocusState.ACTIVE && before.state.transition == Transition.RESUME &&
            before.state.priorStateFingerprintOrNull?.let(::stateRef) == displacedStateRef &&
            before.state.causeOwnerRef == releaseOwnerRef && before.state.causeRef == releaseRef &&
            before.state.causeBindingRefs == releaseBindings &&
            before.state.revalidatedSourceBindingRefs == revalidated
        ) return MutationResult("IDEMPOTENT_RESUMED", false, before)

        require(before.state.state == FocusState.DISPLACED && before.stateRef == displacedStateRef) {
            "working focus resume requires exact current displacement"
        }
        val revision = requireNotNull(before.revisionOrNull)
        require(revalidated == revision.sourceBindingRefs) {
            "working focus resume source bindings are not exact/current"
        }
        val state = formState(
            lineageRef, threadRef, FocusState.ACTIVE, Transition.RESUME, revision, before,
            releaseOwnerRef, releaseRef, releaseBindings, revalidated, formedAt,
        )
        return commit(before, revision, state, "RESUMED_COMMITTED")
    }

    fun clear(
        lineageRef: String,
        threadRef: String,
        causeOwnerRef: String,
        causeRef: String,
        causeBindingRefs: List<String>,
        formedAt: Instant = Instant.now(),
    ): MutationResult {
        requireRef(causeOwnerRef, "causeOwnerRef")
        requireRef(causeRef, "causeRef")
        val bindings = normalizedRefs(causeBindingRefs, "causeBindingRefs")
        val before = requireNotNull(readCurrent(lineageRef, threadRef)) {
            "working focus clear requires existing current state"
        }
        if (before.state.state == FocusState.CLEARED) return MutationResult("ALREADY_CLEARED", false, before)
        val state = formState(
            lineageRef, threadRef, FocusState.CLEARED, Transition.CLEAR, null, before,
            causeOwnerRef, causeRef, bindings, emptyList(), formedAt,
        )
        return commit(before, null, state, "CLEARED_COMMITTED")
    }

    private fun commit(
        expectedBefore: Current?,
        revisionOrNull: FocusRevision?,
        state: StateRecord,
        successState: String,
        formationOrNull: FormationBinding? = null,
    ): MutationResult {
        val paths = paths(state.lineageRef, state.threadRef)
        VexOwnedPath.ensureDirectory(home, paths.root)
        return VexExclusiveFileLease.acquire(paths.lock).use {
            require(readCurrent(state.lineageRef, state.threadRef) == expectedBefore) {
                "working focus currentness changed before commit"
            }
            formationOrNull?.let {
                writeImmutable(paths.formations.resolve(sha256(it.formationRef) + ".json"), it)
            }
            revisionOrNull?.let {
                writeImmutable(paths.revisions.resolve(it.semanticFingerprint + ".json"), it)
            }
            writeImmutable(paths.states.resolve(state.semanticFingerprint + ".json"), state)
            val head = Head(
                HeadSchema, "CURRENT_WORKING_FOCUS_HEAD", EffectOwnerRef,
                state.lineageRef, state.threadRef, state.semanticFingerprint, state.formedAt,
            )
            VexDurableContractFile.write(home, paths.head, head)
            val current = requireNotNull(readCurrent(state.lineageRef, state.threadRef))
            require(current.head == head && current.state == state && current.revisionOrNull == revisionOrNull) {
                "working focus commit readback mismatch"
            }
            MutationResult(successState, true, current)
        }
    }

    private fun formFormationBinding(revision: FocusRevision): FormationBinding {
        val payload =
            FormationPayload(
                FormationSchema,
                "WORKING_FOCUS_FORMATION_BINDING",
                EffectOwnerRef,
                revision.lineageRef,
                revision.threadRef,
                revision.formationRef,
                revision.workingFocusRef,
                revision.semanticFingerprint,
            )
        return FormationBinding(
            payload.schemaVersion,
            payload.truthClass,
            payload.effectOwnerRef,
            payload.lineageRef,
            payload.threadRef,
            payload.formationRef,
            payload.workingFocusRef,
            payload.revisionFingerprint,
            semanticHash(payload),
        )
    }

    private fun formRevision(
        draft: VexLifeWorkingFocus.Draft,
        workingFocusRef: String,
        prior: FocusRevision?,
    ): FocusRevision {
        val payload = RevisionPayload(
            RevisionSchema, "NON_SEMANTIC_WORKING_FOCUS_REVISION", EffectOwnerRef,
            draft.lineageRef, draft.threadRef, workingFocusRef, draft.formationRef,
            draft.subjectScopeRefs, draft.objective, draft.currentQuestionOrNull,
            draft.retainedRelationships, draft.openEdges, draft.nextProbeOrNull,
            draft.sourceBindingRefs, draft.sourceRefs, prior?.semanticFingerprint,
        )
        val fingerprint = semanticHash(payload)
        return FocusRevision(
            payload.schemaVersion, payload.truthClass, payload.effectOwnerRef, payload.lineageRef,
            payload.threadRef, payload.workingFocusRef, payload.formationRef, payload.subjectScopeRefs,
            payload.objective, payload.currentQuestionOrNull, payload.retainedRelationships,
            payload.openEdges, payload.nextProbeOrNull, payload.sourceBindingRefs, payload.sourceRefs,
            payload.priorRevisionFingerprintOrNull, fingerprint,
        )
    }

    private fun formState(
        lineageRef: String,
        threadRef: String,
        state: FocusState,
        transition: Transition,
        revision: FocusRevision?,
        prior: Current?,
        causeOwnerRef: String,
        causeRef: String,
        causeBindingRefs: List<String>,
        revalidatedSourceBindingRefs: List<String>,
        formedAt: Instant,
    ): StateRecord {
        val payload = StatePayload(
            StateSchema, "WORKING_FOCUS_STATE_TRANSITION", EffectOwnerRef,
            lineageRef, threadRef, state, transition, revision?.workingFocusRef,
            revision?.semanticFingerprint, prior?.state?.semanticFingerprint,
            causeOwnerRef, causeRef, causeBindingRefs, revalidatedSourceBindingRefs,
            formedAt.toString(),
        )
        validateStatePayload(payload)
        return StateRecord(
            payload.schemaVersion, payload.truthClass, payload.effectOwnerRef, payload.lineageRef,
            payload.threadRef, payload.state, payload.transition, payload.workingFocusRefOrNull,
            payload.revisionFingerprintOrNull, payload.priorStateFingerprintOrNull,
            payload.causeOwnerRef, payload.causeRef, payload.causeBindingRefs,
            payload.revalidatedSourceBindingRefs, payload.formedAt, semanticHash(payload),
        )
    }

    private fun validateFormation(value: FormationBinding) {
        require(
            value.schemaVersion == FormationSchema &&
                value.truthClass == "WORKING_FOCUS_FORMATION_BINDING" &&
                value.effectOwnerRef == EffectOwnerRef,
        ) {
            "working focus formation identity is invalid"
        }
        requireRef(value.lineageRef, "formation lineageRef")
        requireRef(value.threadRef, "formation threadRef")
        requireRef(value.formationRef, "formation formationRef")
        requireRef(value.workingFocusRef, "formation workingFocusRef")
        requireSha256(value.revisionFingerprint, "formation revisionFingerprint")
        val payload =
            FormationPayload(
                value.schemaVersion,
                value.truthClass,
                value.effectOwnerRef,
                value.lineageRef,
                value.threadRef,
                value.formationRef,
                value.workingFocusRef,
                value.revisionFingerprint,
            )
        require(value.semanticFingerprint == semanticHash(payload)) {
            "working focus formation semantic identity is invalid"
        }
    }

    private fun validateRevision(value: FocusRevision) {
        require(value.schemaVersion == RevisionSchema &&
            value.truthClass == "NON_SEMANTIC_WORKING_FOCUS_REVISION" &&
            value.effectOwnerRef == EffectOwnerRef
        ) { "working focus revision identity is invalid" }
        VexLifeWorkingFocus.Draft(
            value.lineageRef, value.threadRef, value.formationRef,
            if (value.priorRevisionFingerprintOrNull == null) null else value.workingFocusRef,
            value.subjectScopeRefs, value.objective, value.currentQuestionOrNull,
            value.retainedRelationships, value.openEdges, value.nextProbeOrNull,
            value.sourceBindingRefs, value.sourceRefs,
        )
        require(value.subjectScopeRefs == value.subjectScopeRefs.distinct().sorted() &&
            value.retainedRelationships == value.retainedRelationships.sortedBy { it.relationshipRef } &&
            value.openEdges == value.openEdges.sortedBy { it.edgeRef } &&
            value.sourceBindingRefs == value.sourceBindingRefs.distinct().sorted() &&
            value.sourceRefs == value.sourceRefs.distinct().sorted()
        ) { "working focus revision representation is not canonical" }
        value.priorRevisionFingerprintOrNull?.let { requireSha256(it, "priorRevisionFingerprintOrNull") }
        val payload = RevisionPayload(
            value.schemaVersion, value.truthClass, value.effectOwnerRef, value.lineageRef,
            value.threadRef, value.workingFocusRef, value.formationRef, value.subjectScopeRefs,
            value.objective, value.currentQuestionOrNull, value.retainedRelationships, value.openEdges,
            value.nextProbeOrNull, value.sourceBindingRefs, value.sourceRefs,
            value.priorRevisionFingerprintOrNull,
        )
        require(value.semanticFingerprint == semanticHash(payload)) {
            "working focus revision semantic identity is invalid"
        }
    }

    private fun validateState(value: StateRecord) {
        require(value.schemaVersion == StateSchema && value.truthClass == "WORKING_FOCUS_STATE_TRANSITION" &&
            value.effectOwnerRef == EffectOwnerRef
        ) { "working focus state identity is invalid" }
        val payload = StatePayload(
            value.schemaVersion, value.truthClass, value.effectOwnerRef, value.lineageRef,
            value.threadRef, value.state, value.transition, value.workingFocusRefOrNull,
            value.revisionFingerprintOrNull, value.priorStateFingerprintOrNull,
            value.causeOwnerRef, value.causeRef, value.causeBindingRefs,
            value.revalidatedSourceBindingRefs, value.formedAt,
        )
        validateStatePayload(payload)
        require(value.semanticFingerprint == semanticHash(payload)) {
            "working focus state semantic identity is invalid"
        }
    }

    private fun validateStatePayload(value: StatePayload) {
        requireRef(value.lineageRef, "state lineageRef")
        requireRef(value.threadRef, "state threadRef")
        requireRef(value.causeOwnerRef, "state causeOwnerRef")
        requireRef(value.causeRef, "state causeRef")
        requireRefs(value.causeBindingRefs, "state causeBindingRefs", false)
        requireRefs(value.revalidatedSourceBindingRefs, "state revalidatedSourceBindingRefs", true)
        require(value.causeBindingRefs == value.causeBindingRefs.distinct().sorted() &&
            value.revalidatedSourceBindingRefs == value.revalidatedSourceBindingRefs.distinct().sorted()
        ) { "working focus state representation is not canonical" }
        value.priorStateFingerprintOrNull?.let { requireSha256(it, "priorStateFingerprintOrNull") }
        Instant.parse(value.formedAt)
        when (value.state) {
            FocusState.ACTIVE -> {
                require(value.transition in setOf(Transition.SET_CURRENT, Transition.RESUME) &&
                    value.workingFocusRefOrNull != null && value.revisionFingerprintOrNull != null
                ) { "ACTIVE working focus bindings are invalid" }
                if (value.transition == Transition.RESUME) {
                    require(value.revalidatedSourceBindingRefs.isNotEmpty()) {
                        "RESUME requires exact revalidated source bindings"
                    }
                } else {
                    require(value.revalidatedSourceBindingRefs.isEmpty() && value.causeOwnerRef == EffectOwnerRef) {
                        "SET_CURRENT source/cause bindings are invalid"
                    }
                }
            }
            FocusState.DISPLACED -> require(
                value.transition == Transition.DISPLACE && value.workingFocusRefOrNull != null &&
                    value.revisionFingerprintOrNull != null && value.revalidatedSourceBindingRefs.isEmpty(),
            ) { "DISPLACED working focus bindings are invalid" }
            FocusState.CLEARED -> require(
                value.transition == Transition.CLEAR && value.workingFocusRefOrNull == null &&
                    value.revisionFingerprintOrNull == null && value.revalidatedSourceBindingRefs.isEmpty(),
            ) { "CLEARED working focus bindings are invalid" }
        }
        value.workingFocusRefOrNull?.let { requireRef(it, "workingFocusRefOrNull") }
        value.revisionFingerprintOrNull?.let { requireSha256(it, "revisionFingerprintOrNull") }
    }

    private fun revisionMatchesDraft(revision: FocusRevision, draft: VexLifeWorkingFocus.Draft): Boolean =
        revision.lineageRef == draft.lineageRef && revision.threadRef == draft.threadRef &&
            revision.formationRef == draft.formationRef && revision.subjectScopeRefs == draft.subjectScopeRefs &&
            revision.objective == draft.objective && revision.currentQuestionOrNull == draft.currentQuestionOrNull &&
            revision.retainedRelationships == draft.retainedRelationships && revision.openEdges == draft.openEdges &&
            revision.nextProbeOrNull == draft.nextProbeOrNull &&
            revision.sourceBindingRefs == draft.sourceBindingRefs && revision.sourceRefs == draft.sourceRefs &&
            ((revision.priorRevisionFingerprintOrNull == null && draft.priorWorkingFocusRefOrNull == null) ||
                (revision.priorRevisionFingerprintOrNull != null &&
                    draft.priorWorkingFocusRefOrNull == revision.workingFocusRef))

    private fun focusRef(draft: VexLifeWorkingFocus.Draft): String =
        "focus.vexlife-prod." + sha256(draft.lineageRef + "|" + draft.threadRef + "|" + draft.formationRef).take(32)

    private fun paths(lineageRef: String, threadRef: String): Paths {
        requireRef(lineageRef, "lineageRef")
        requireRef(threadRef, "threadRef")
        val root = VexOwnedPath.resolveInside(
            home,
            "working-focus/" + sha256(lineageRef + "|" + threadRef).take(32),
        )
        return Paths(
            root,
            root.resolve("formations"),
            root.resolve("revisions"),
            root.resolve("states"),
            root.resolve("head.json"),
            root.resolve("owner.lock"),
        )
    }

    private fun readFormation(
        paths: Paths,
        formationRef: String,
    ): FormationBinding? =
        VexDurableContractFile.read(
            home,
            paths.formations.resolve(sha256(formationRef) + ".json"),
        )

    private fun normalizedRefs(values: List<String>, label: String): List<String> {
        requireRefs(values, label, false)
        return values.sorted()
    }

    private fun requireRefs(values: List<String>, label: String, allowEmpty: Boolean) {
        require(allowEmpty || values.isNotEmpty()) { "$label must not be empty" }
        require(values.distinct().size == values.size) { "$label must be unique" }
        values.forEachIndexed { index, value -> requireRef(value, "$label[$index]") }
    }

    private inline fun <reified T> writeImmutable(path: Path, value: T) {
        val existing = VexDurableContractFile.read<T>(home, path)
        if (existing == null) VexDurableContractFile.write(home, path, value)
        else require(existing == value) { "working focus immutable address contains different truth" }
    }

    private fun requireRef(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "$label must be one portable stable ref"
        }
    }

    private fun requireSha256(value: String, label: String) {
        require(value.matches(Regex("[0-9a-f]{64}"))) { "$label must be one lowercase SHA-256" }
    }

    private inline fun <reified T> semanticHash(value: T): String =
        sha256(VexContractJson.canonicalEncode(value))

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun stateRef(fingerprint: String): String = "state.vexlife-prod.working-focus.$fingerprint"
    private fun revisionRef(fingerprint: String): String = "revision.vexlife-prod.working-focus.$fingerprint"
}

// [VXG RealForever]
