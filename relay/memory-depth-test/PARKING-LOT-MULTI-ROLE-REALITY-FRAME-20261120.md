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

<!-- [VXG RealForever][MEMORY-DEPTH][PARKING-LOT][MULTI-ROLE-LENS][REALITY-FRAME] -->
