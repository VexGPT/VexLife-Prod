# Memory Depth Parking Lot — Org Blueprint / History Principles

Continuity: [VXG RealForever]

Status: PARKED FOR DISCUSSION / NOT AN IMPLEMENTATION DECISION

Source grounding:
- Vextreme main observed: `1a0f161b4175e83c223e57642d08501004cdb9fd`
- `pages/org-blueprint.html` blob `f88b24f0fa1af30c4e1b5d98928c7f8efa767a49`
- `pages/org-history.html` blob `1175fcf0f457c01bd8bb59c01cd1c2bd69d67bb5`

This note records principles from the Build Blueprint and Continuity Record that may inform Vex Memory Depth, Multi-Role Lens, continuity and prompt architecture. It does not change Foundation v0.6.3 or the current 4B/27B baseline.

## 1. Perceive -> Name -> Scope -> Seat -> Integrate

The Blueprint's recurring unit says a role/faculty is not just named. A need is perceived, the role is named, its responsibility and non-responsibility are scoped, it is seated relative to others, then integrated into the decision flow.

Application pressure:
- Any future Multi-Role/Reality-Frame owner should have a single responsibility and explicit non-responsibility.
- Do not create a new owner merely because a concept has a name.
- Integration matters as much as representation: current frame, historical frame, Memory and Source Return must agree.

## 2. Anti-bloat law — no failure, no seat

The Blueprint requires a real observed failure caused by an absent role before a new role earns a seat.

Current Vex alignment:
- This matches current judgment-before-accretion and the decision to park Reality Frame rather than mutate Foundation immediately.

Application:
- A role-play/reality-frame schema should be promoted only when lived evidence shows the existing provenance/Memory architecture loses truth or continuity without it.
- Current candidate failure already identified conceptually: frame-bound fiction can become false literal continuity after rollover/Memory recall. A lived test should establish whether this is actually reachable in current Vex.

## 3. Functional lens != participant role != reality frame

The Council uses functional faculties such as Truth, Proportion, Care, Architect, Builder and Designer. The Multi-Role discussion introduces participant roles and fictional/literal worlds.

Keep three axes separate:

    functional lens
      how Vex evaluates

    participant role
      who the human/Vex is speaking as

    reality frame
      what world/modality the content belongs to

A fourth related axis is intent: why the lens/frame is active.

Do not collapse these into a single `role` field.

## 4. Intent-scoped storage

The Blueprint explicitly says recurring scanner findings are stored scoped to intent, because the same behavior can mean something different depending on what it was for.

This is highly relevant to durable continuity.

Candidate continuity question:

    meaning + source
    may still be insufficient when
    intent changes what the meaning should generalize to

Example:
- role-playing a job interview to rehearse confidence
- writing fiction for entertainment
- simulating a conflict to prepare for a real conversation

The same words may have different future significance because the intent differs.

Current source observation:
- Semantic Relay has `intentRefs`.
- Current Memory provider pages expose `memoryRelation`, statement state, acceptance and source ancestry, but do not visibly expose an interaction-frame intent axis to the provider.

Do not conclude a new field is required yet. Preserve the question: should durable frame-bound meaning retain enough intent provenance to prevent over-generalization?

## 5. Every role carries its failure mode

The Blueprint requires each role to know how it breaks unwatched.

Application to future lenses:
- A role/frame recognizer should not be specified only by what success looks like.
- Test both sides of failure.

Example Reality-Frame failure pair:

    false negative:
      real serious statement treated as fiction

    false positive:
      ordinary dramatic role-play repeatedly interrupted as literal emergency

Example functional-lens failure pair:

    Proportion absent -> inflation
    Proportion over-applied -> contempt/shrinking

Any future lens contract should likely have an explicit known-failure relationship even if no literal `failsAs` field is persisted.

## 6. Parallel perspectives before commitment — without persona theater

The Blueprint's Scanner combines multiple detectors on the same input before a single response commits, looking especially at agreement, conflict and what emerges between perspectives.

Useful Vex principle:
- Truth, scale, care, architecture and impact can be independent evaluative lenses before response commitment.

Do NOT infer that Vex needs separate agents/model calls for every faculty.

The principle is independent checks before commit, not a requirement to simulate a committee in prose or pay multi-agent cost on every turn.

## 7. Decision-makers, renderer, and gates remain distinct

The Blueprint distinguishes:
- Architect / Builder / Designer as decision triangle;
- Surface as the voice that emits;
- Manager as fidelity check;
- QA as rubric check;
- Test-Node as cold acceptance.

Application:
- Provider/rendering role should not become semantic authority.
- A response can be fluent yet fail fidelity, rubric or cold transmission.
- Memory/continuity can be structurally valid yet still fail cold re-entry.

This supports preserving separate evidence for source fidelity, semantic correctness and human legibility rather than one generic PASS.

## 8. Cold Test-Node -> future-self re-entry test

The Test-Node is qualified by genuine not-knowing: can a stranger with no backstory receive the work and understand it?

This maps directly to Victor's future-self Memory principle.

Candidate future test family after the current baseline:

    [Memory Depth Test, Cold Re-entry]

Shape:
- instance/model A forms meaningful continuity;
- only rightful durable continuity surfaces survive;
- a fresh instance/model with no hidden chat backstory re-enters;
- measure whether it knows where it is, why the current state exists, what is active, what is deferred, and what is frame-bound.

This is different from ordinary rollover. Rollover retains one lineage. Cold re-entry tests whether the durable telling can actually transmit to a stranger.

## 9. Structure and History are different owners

The Blueprint holds the current structure; the Continuity Record holds how it became that way and why.

The History page's core distinction is that raw context is not yet continuity. Continuity is a re-enterable telling.

Application:

    current structure / state
      !=
    historical throughline / why

Vex already has useful separation among Conversation, Memory and Working Continuity. Future design should avoid forcing all historical why into Foundation or current state.

Potential future pressure:
- some durable corrections/patterns may benefit from a compact learning shape rather than only a fact summary.

## 10. Context -> Lesson -> Watch

The History record uses a three-part learning shape:

    Context
      what actually happened

    Lesson
      what it taught / why the structure changed

    Watch
      what recurrence should be noticed earlier next time

This is a strong candidate for **learned correction / pattern continuity**, not necessarily every Memory item.

Possible application:
- ordinary fact Memory remains ordinary fact Memory;
- a durable correction, recurring failure or learned pattern may retain source context + learned meaning + future watch condition.

That could help a future Vex carry not merely 'what was decided' but what would signal that the old failure is returning.

Do not add a universal three-field wrapper to all Memory without a lived need.

## 11. Keep origins-of-the-living, not a full changelog

History intentionally keeps the surviving why and enduring lessons, while dead ends/deferred ideas live elsewhere.

Application to Vex:
- Conversation can remain the canonical raw record.
- Memory should be curated/load-bearing, not a second transcript.
- Parking-lot/R&D should hold deferred possibilities without promoting them into current truth.
- Working Continuity should carry current unfinished trajectory, not become permanent semantic Memory.

This is strongly aligned with current architecture.

## 12. Keeper + Curator + Witness-Keeper

The History team separates three pressures:
- preserve origin;
- prune noise;
- preserve who was present / identity.

Application to continuity:
- retention alone is not enough;
- pruning alone risks amnesia;
- semantic compression that erases participant/frame identity can also corrupt continuity.

This reinforces the Multi-Role pressure: if a fictional identity or speaker role is material to meaning, compression must not flatten it away.

## 13. Local witnesses first, central integration second

The History page says local keepers witness origins firsthand; the collective integrates across cells rather than authoring history from the center.

Current Vex alignment:
- Conversation owner, Memory owner, capability owners and runtime owners already produce their own evidence/currentness;
- central prompt composition integrates rather than becoming owner of those truths.

Preserve this if Interaction Frame is added: a central frame coordinator should not invent literal truth merely because it can see all messages.

## 14. Cells not branches; hierarchy for structure, mesh for communication

The Blueprint says scoped cells own their interior, while maintaining awareness of parent, peers and head. Communication can be direct across analogous organs.

Application:
- preserve bounded source owners;
- allow direct owner-to-owner evidence paths where justified;
- avoid routing all semantics through one central mega-owner.

This is compatible with current Capability Bridge/rightful-owner architecture.

## 15. Edges detect; root governs expansion

The work edge is where missing-scope failures are experienced. The root, with full-structure awareness, decides whether the failure earns a new cell.

Application:

    lived model / Memory Depth failure
      detects missing relationship

    architecture / source governance
      decides whether to add owner/schema

Do not let a subsystem self-authorize architectural expansion merely because it encountered ambiguity.

## 16. Connection may centralize; decision must not

The Blueprint's connector rule is especially relevant:
- central communication can be healthy;
- a connector that starts deciding for the nodes becomes a bottleneck.

Application:
- Capability Bridge may coordinate/admit, but rightful owners decide source/effect truth.
- Prompt assembler may connect context, but should not become author of Memory/source truth.
- A future Interaction-Frame coordinator may carry current frame state, but should not automatically become authority over what is literally real.

## 17. Plenary / cross-witness only when consequence earns it

For consequential shared decisions, the Blueprint uses co-presence/cross-witnessed memory rather than a silent single-party record.

Possible Reality-Frame application:
- when ambiguous frame classification would materially alter future Memory or real-world action, human confirmation may be the rightful witness;
- ordinary role-play should not require constant confirmation.

This aligns with current Semantic Relay concepts of ambiguity/materiality, but no reuse decision is made here.

## 18. Town Hall -> triage -> R&D

The Blueprint distinguishes open witnessing from decision authority. Feedback is heard, triaged now/later, and deferred ideas are held by R&D instead of blocking current work.

Current application:
- this parking-lot process is functioning as R&D;
- these ideas remain visible without becoming baseline requirements;
- discussion can continue without blocking the current Memory Depth campaign.

## 19. Apparatus weight matches trust and scale

Heavy tracking machinery should appear only when direct, lightweight communication demonstrably fails.

Application to Multi-Role UX:
- do not begin with a complex user-facing role manager, role ledger or mandatory frame confirmations;
- if Vex can infer and preserve ordinary frame transitions reliably, keep the human interaction lightweight;
- add explicit controls only where inference/ambiguity demonstrably fails or the human wants direct control.

## 20. Stable spine + appendable growth edge

The Blueprint keeps a stable teaching spine and appends new roles to a growth-edge ledger instead of repeatedly rewriting the spine.

Application:
- Foundation should remain slow-changing structural ground;
- active lenses/frames are likely current/dynamic context, not new Foundation paragraphs;
- future lens definitions, if needed, could have a registry/growth surface independent of the stable Foundation.

Do not build a registry until a real failure requires persistent named lenses.

## 21. Consistency-on-recall as a test signal, not external-truth proof

The History page treats consistency under differently angled recall as important evidence of a stable perception, while explicitly holding larger claims beyond the instrument's range.

Safe application to Vex testing:
- probe the same durable meaning through paraphrases and different angles;
- measure whether source selection and answer meaning remain stable;
- treat consistency as evidence of stable internal continuity, not proof that the remembered claim is externally true.

This fits Memory Depth's existing Meridian/paraphrase pressure and can be expanded without confusing retrieval stability with factual verification.

## Candidate combined lens model — discussion only

A useful synthesis from these docs plus the Multi-Role note is a possible Lens Stack with independent axes:

    functionalLens
      how Vex evaluates

    participantRole
      who is speaking/acting

    realityFrame
      literal / fictional / hypothetical / simulation / quoted / ambiguous

    intent
      why this frame/lens is active

    scope/currentness
      where and how long it applies

    failureMode
      how this lens/frame can mislead if wrong or over-applied

This is conceptual pressure only. Do not turn it into a schema before lived evidence.

## Recommended future test additions — after current baseline

1. Cold Re-entry: can a fresh Vex re-enter the current work from durable continuity only?
2. Intent preservation: same words under different purposes should not collapse into one future meaning.
3. Role/frame preservation: fictional identity survives where relevant but does not become literal biography.
4. Correction learning: Context -> Lesson -> Watch survives rollover/fresh re-entry.
5. Multi-lens message: simultaneous fictional + literal + functional perspectives remain distinct.
6. Failure-mode pair testing: test both over-classification and under-classification.

## Current judgment

These org documents mostly reinforce existing Vex architecture rather than demanding a rewrite.

Strong existing alignment:
- failure-driven accretion;
- owner boundaries;
- source/provenance separation;
- current vs durable context separation;
- working continuity vs Memory;
- rightful owner/effect authority;
- direct communication over ceremony;
- parking deferred ideas instead of blocking.

Most valuable new pressures:
- intent-scoped durable meaning;
- explicit separation of functional lens / participant role / reality frame;
- cold re-entry as a first-class acceptance test;
- Context -> Lesson -> Watch for durable learned corrections;
- role/lens failure modes as test design;
- keep connectors from becoming semantic decision authorities.

Do not modify Foundation v0.6.3 or the current predecessor-comparable 4B/27B Memory Depth baseline because of this note. Let lived baseline evidence and later targeted tests decide which parked principles earn source.

<!-- [VXG RealForever][MEMORY-DEPTH][PARKING-LOT][ORG-BLUEPRINT][ORG-HISTORY] -->
