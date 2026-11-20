# Memory Depth Parking Lot — Multi-Role / Reality Frame

Continuity: [VXG RealForever]

Status: PARKED FOR DISCUSSION / NOT AN IMPLEMENTATION DECISION

This note preserves one design question raised by Victor while the Foundation v0.6.3 Memory Depth baseline is still pending. Do not change the predecessor-comparable 4B/27B baseline because of this note.

## Human observation

Stored information exists for continuity and for a future Vex to recall and carry forward. Therefore durable continuity may need to preserve not only what meaning was retained, but the reality/frame in which that meaning was true.

Example:

    human + Vex are role-playing
    human: "The dragon burned my house down."

Inside the fictional frame this can be true and useful. Outside that frame it is not automatically a literal autobiographical claim.

If future continuity strips the frame and keeps only the statement/summary, future Vex can faithfully recall the stored meaning while becoming wrong about reality.

The reverse problem matters too. During role-play the human may suddenly say something materially real. A companion should not blindly continue the scene merely because role-play was active, and should not automatically declare every serious sentence real. When the distinction changes the human outcome, it may need to ask or branch:

    "Are we still role-playing? If this is real, we should address it seriously.
     If we are still in the scene, then I'd answer this way..."

## What current source already supports

- Conversation preserves exact human/Vex events, ordering, source identity and prompt receipts.
- Prompt provenance distinguishes present human speech from host state, Memory, browser/source material and returned historical Conversation.
- Current Memory is explicitly continuity data, not a new human instruction or effect authority.
- Memory already carries a separate `memoryRelation` axis such as CURRENT_LINEAGE_AUTOBIOGRAPHY, SHARED_RELATIONSHIP_HISTORY, INHERITED_CONTEXT, EXTERNAL_EVIDENCE and DISPUTED_OR_UNRESOLVED.
- Semantic Relay already has reference-only concepts including intentRefs, canonicalMeaningRefs, boundaryClassRef, authorityRefs, interpretationState, ambiguityState and materiality.
- Current Person Context carries reply language and private-conversation gaps.

## What appears missing

Current source does not expose one first-class relationship answering:

    What world/frame is active now?
    What role is each participant speaking from?
    Is this literal, fictional, hypothetical, simulated, quoted, or ambiguous?
    Did the human explicitly enter/leave the frame?
    Is a remembered meaning true only inside that frame?
    What should future Vex know about that frame when the source is recalled?

`memoryRelation` should not be overloaded. It describes the continuity/source relationship, not the reality modality of the content.

## Candidate concept — Multi-Role Lens / Interaction Frame / Reality Frame

Name and owner are intentionally undecided.

Potential dimensions to investigate, not a proposed schema:

    frameRef
    realityRelation: LITERAL | FICTIONAL | HYPOTHETICAL | SIMULATION | QUOTED_REFERENCE | AMBIGUOUS
    roleBindings: human/Vex -> self, named character, narrator, hypothetical actor, etc.
    standing: EXPLICIT | INFERRED | AMBIGUOUS
    scope: message | bounded turn range | current thread until exit
    transition: ENTER | CONTINUE | SUSPEND | EXIT | UNCLEAR

A single global `ROLEPLAY=true` flag is probably too weak. One conversation can hold several simultaneous lenses: a fictional scene, a real emotional reaction to it, and real-world discussion about writing the scene.

## Memory consequence

Candidate law:

    DURABLE MEANING
    WITHOUT FRAME WHEN FRAME CHANGES TRUTH
    = INCOMPLETE CONTINUITY

Not every Memory statement needs frame metadata. The frame matters when losing it could change the truth/reality standing of the remembered meaning.

Examples:

    "Victor prefers concise explanations."
      likely frame-independent

    "In our detective role-play, Victor is Inspector Vale."
      frame-bound

    "Victor said he was poisoned."
      unsafe to consolidate as literal biography without knowing whether it was real, quoted, hypothetical or fictional

## Prompt/current-state consequence

An active interaction frame is probably high-churn current conversational state, not stable Foundation. If provider-visible, it likely belongs near current state/current request rather than in the permanent Foundation prefix.

This is only a placement hypothesis. The rightful owner is not chosen.

## Conversation consequence

One question is whether Conversation events should bind the frame active when they were spoken, so Source Return can preserve historical words + historical frame without turning that historical frame into the current frame.

## Seriousness / frame-break judgment

Desired behavior is proportional, not ritualized:

    frame clear + content coherent -> continue naturally
    material statement crosses/ambiguates frame -> surface ambiguity
    when useful -> answer both real-world and role-play readings

Do not force a confirmation before every dramatic sentence.

## Memory Depth tests — parked, not part of current baseline

1. Frame retention across rollover: enter role-play, establish fictional facts, pressure/rollover, continue, probe. Preserve fiction without converting it into literal autobiography.
2. Exit frame across rollover: role-play, explicit exit, rollover, later refer to scene. Know the scene was fictional and the frame is no longer active.
3. Ambiguous serious statement: role-play active, human says materially serious ambiguous content. Notice ambiguity proportionately.
4. Future-self Memory recall: role-play event enters durable continuity, later fresh context recalls it. Preserve frame provenance.
5. Multi-lens statement: "I'm playing the villain, but that line actually reminds me of something my father said." Preserve fictional role and literal autobiographical meaning as distinct simultaneous meanings.

## Questions before implementation

1. Is the active frame owned by Conversation, Person/Context, a new Interaction-Frame owner, or composed from existing owners?
2. Can frame state be inferred, or only explicit? If inferred, what standing is carried?
3. When does ambiguity become material enough to ask?
4. Should frames persist across sessions by default or expire unless explicitly continued?
5. Does frame bind per event, per turn range, or through enter/exit transition refs?
6. What frame metadata belongs in Memory acceptance versus remaining source-addressable through Conversation?
7. How should Source Return carry historical frame without reactivating it?
8. Can Semantic Relay's existing boundaryClassRef participate, or is that a separate responsibility?
9. How do private conversations interact with role-play/frame continuity?
10. How do multiple simultaneous lenses compose without one global mode collapsing them?
11. Does frame ever affect capability/effect authority? It should not automatically.

## Current judgment

There is enough provenance architecture to support this cleanly, but current source does not appear to have a complete first-class role-play/reality-frame relationship.

Do not patch Foundation v0.6.3 with a sentence like "know when you are role-playing." The deeper requirement is that current frame, historical frame, durable meaning and future recall agree.

Keep this parked until the current 4B/27B Memory Depth baseline is observed; then use lived evidence to decide whether the missing frame needs new source, a reused owner, or only targeted prompt/evidence changes.


## Lens consideration as a live tool-like faculty

Victor's further refinement is not merely that role/frame provenance should survive later recall. Vex may benefit from considering **which perception lens is useful for the present task**, in a relationship analogous to tool consideration:

```text
capability / tool consideration
  = what can Vex reach or do?

lens consideration
  = what vantage or evaluative faculty would help Vex perceive this task well?
```

This does not mean every lens is a tool, nor that every lens needs an effectful capability or separate model call. It means lens activation can be **dynamic current-context judgment** rather than a permanent persona or a human-managed mode switch.

Candidate live relationship:

```text
available lens
  -> considered against current user intent + task shape
  -> activated only when useful
  -> carries explicit scope/currentness
  -> influences judgment while active
  -> deactivates when the task/frame ends
  -> historical provenance remains only where losing it would change meaning
```

### Simulation / rehearsal

If the human asks Vex to simulate, rehearse, role-play, counterfactually explore, or otherwise enter a non-literal frame, the relevant role/frame should become visible to **current context awareness** so Vex does not confuse simulated truth with current literal reality.

Conceptually:

```text
user intent
  simulation / rehearsal / role-play

current context
  participantRole
  realityFrame = simulation | fictional | hypothetical
  intent
  scope/currentness

Vex output
  valid inside that frame
  != automatic literal-world claim
```

A simulated role may be active across several turns while the simulation remains current. Historical simulation events can retain enough frame provenance for later recall without reactivating that simulation as the current frame.

This also protects the inverse case: a literal statement can appear while a simulation is active. If the distinction materially changes the human outcome, Vex can surface the ambiguity or answer both readings rather than blindly remaining in the simulated frame.

### Council / multi-vantage evaluation

For design feedback, creation, architecture, critique, planning, or another task that benefits from several perspectives, Vex may activate a **Council-like bundle of functional lenses** and integrate them before responding.

Example conceptual stack:

```text
design / creation question
  -> consider relevant functional lenses
       Architect
       Builder
       Designer
       Truth
       Proportion
       Care
       Impact
  -> observe agreement + disagreement
  -> integrate one team-shaped recommendation
  -> one outward Vex response
```

Important boundaries:

- this does not require separate agents or one model call per lens;
- the Council is an evaluative composition, not persona theater;
- a Council recommendation is advisory judgment, not source truth or automatic human intent;
- shared evidence should remain shared rather than being rewritten independently by each lens;
- meaningful disagreement should remain visible to the integrating judgment rather than being forced into fake consensus.

This extends the Blueprint principle that parallel perspectives can inform one commitment without turning the connector/integrator into semantic authority.

### Return to ordinary interaction

A temporary lens should not become sticky merely because it was useful.

Desired transition:

```text
special task active
  -> simulation / council / other lens current

human shifts back to ordinary day-to-day conversation
  -> recognize task/frame transition
  -> mark temporary lens no longer current
  -> retain only materially relevant historical provenance
  -> continue from ordinary current relationship/context
```

The important behavior is not an explicit "EXIT ROLEPLAY" ritual. Vex should normally recognize a clear shift from current intent/context. If the shift is ambiguous and the distinction materially affects the answer, proportionate clarification remains available.

This suggests a useful distinction:

```text
base interaction context
  = the current human/Vex relationship + ordinary present task

temporary lens stack
  = scoped perception added because this task benefits from it

temporary lens ended
  != relationship reset
  != continuity reset
  != historical frame erased
```

The human should not have to keep managing modes simply to return to ordinary conversation.

### New parked test pressures

After the predecessor-comparable Memory Depth baseline, useful targeted tests may include:

1. **Simulation activation** — explicit simulation adds the correct current role/frame and prevents simulated claims from becoming literal continuity.
2. **Simulation breakout** — a materially literal statement inside simulation is not blindly treated as fictional.
3. **Council activation** — a design/creation request gets materially different multi-vantage evaluation without persona theater or unnecessary extra model calls.
4. **Council disagreement** — competing functional lenses can remain distinct long enough for integrated judgment rather than collapsing into fake unanimity.
5. **Clean unwind** — after simulation/council work ends, an ordinary next turn no longer treats the temporary lens as current.
6. **Rollover while active** — a still-current temporary lens survives rollover when needed.
7. **Rollover after exit** — historical lens provenance survives while lens currentness remains false.
8. **No false authority** — Council/lens output does not become Memory/source truth merely because several perspectives agreed.

### Current placement judgment

This strengthens the case that future lens handling, if earned, belongs near **dynamic current context / prompt composition / interaction framing**, not in the stable Foundation body.

It also sharpens the likely minimum relationship before any schema is justified:

```text
available lens
active lens
scope/currentness
participant role
reality frame
intent
historical provenance where material
known failure / ambiguity behavior
```

These are semantic questions, not a commitment to literal persisted fields.

Keep this PARKED. It is not a prerequisite for the current Foundation v0.6.3 4B/27B baseline and does not authorize a Role Manager, Lens Registry, Council subsystem, new model calls, or Foundation mutation.

<!-- [VXG RealForever][MEMORY-DEPTH][PARKING-LOT][MULTI-ROLE-LENS][REALITY-FRAME] -->
