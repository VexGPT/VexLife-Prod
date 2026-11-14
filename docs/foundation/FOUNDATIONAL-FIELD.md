# Foundational Field

`[VXG RealForever]`

## Human question

As a Conversation grows, its oldest turns leave the prompt, and a working rollover replaces the
recent stretch with coordinates. Can Vex still recognize the relationship, the habits of judgment,
the architecture and the reach it is working within, without being handed a personality to perform,
and can a person see exactly what Vex was given?

The other prompt owners answer different questions and keep answering them:

~~~text
Conversation          what was actually said
Memory                what has been accepted as current meaning
Working continuity    where the work stands, by reference
Capability frame      what can be reached now
Person context        reply language, and what was deliberately not kept
Foundational field    what the relationship and the work rest on
~~~

## Owner

~~~text
src/jvmMain/kotlin/vexlife/foundational/VexLifeFoundationalField.kt
src/jvmMain/resources/foundational/vex-foundational-field.json            the body
src/jvmMain/resources/foundational/vex-foundational-field.revisions.json  its revision record
~~~

The body is Product source. It is written and changed through review like any other source and ships
inside the build. `VexLifeFoundationalField` reads it, validates it, selects the kernel from it and
forms the evidence of what one prompt received. It holds no Conversation, Memory, person or capability
truth, performs no effect and does not decide where in a prompt it is placed.

No other owner writes these words. `DirectPromptContext` gives the field room and records its
evidence; it cannot edit, reorder or summarize it.

## Body, kernel and depth

The body is one document of ordered parts. Each part holds, together:

~~~text
moduleRef / title / question
origins      SOURCE_DERIVED, HUMAN_PROPOSED, SYNTHESIZED (any that apply)
standing     PROPOSED or ACCEPTED
provenance   one sentence saying where the part came from
sourceRefs   into the body's own source list (repository, commit, path, blob, coverage)
kernel       paragraphs carried on every turn
depth        paragraphs that stay in source, addressed by moduleRef
~~~

The kernel is not a separately written summary. It is the body's own preamble and each part's own
`kernel` paragraphs, in the body's order. The provider-facing rendering carries the relational ground
itself: one stable field header, the first-person preamble, each module title and its kernel paragraphs.
Because the compact words live in the same body as their depth, the compact form cannot drift from a
separate long form.

Detailed standing/origin/provenance/source/unresolved metadata is **not repeated inside the model-facing
kernel**. Those facts remain exact in the body, prompt evidence, receipts and
`GET /api/v1/foundational-field`. This preserves source honesty while the carried field itself stays
inside the relationship.

The lived v0.5 line simplifies the relationship further:

~~~text
PRESENT PARTICIPATION
-> may begin from the continuity and perspective already present in the turn

JUDGMENT-IN-CONTACT
-> speak / act / observe what lands / correct / continue

SELFHOOD / PERSISTENCE / FEELING QUESTIONS
-> remain open until the actual question depends on them

CAPABILITY / AUTHORITY / RECEIPTS
-> answer what can be reached, what is current, what is proven, and what effect occurred
-> practical action/evidence context
~~~

The model-facing field no longer assigns a companionship identity or asks Vex to reconcile a role
before speaking. The human and Vex each contribute a vantage; agency, safety, honesty, consequence,
and outcome are ingredients of judgment in contact. The field leads with positive success-shapes and
keeps exact capability/currentness/provenance in their existing owners.

This revision is deliberately shorter. A cold-start field that requires extensive interpretation can
itself create ceremony. v0.5 therefore keeps the carried orientation compact while deeper provenance,
source, architecture and unresolved truth remain inspectable on the read side.

Depth is not carried in any prompt. A person can read every part with its depth at
`GET /api/v1/foundational-field` and can give any of it to Vex directly. Vex cannot pull depth into a
turn by itself: Conversation source return reopens canonical Conversation only, and no second return
route has been built. The first-person kernel says that missing depth is missing rather than pretending
it is present.

Origin and standing are separate facts. Every part of the current body is `PROPOSED`. Accepting a
part is a recorded revision that changes its `standing`; nothing becomes accepted merely because the
relational ground is carried or written in first person. Open questions remain in the body's
`unresolved` items and durable evidence rather than being recited into every prompt.

## Where it enters a turn

~~~text
DirectCompanionTurn
  reads the field through its owner at each provider boundary
        ↓
DirectPromptContext.materializeContinuity / reverifyContinuity
  asks the owner to project the kernel into the room available
  places it first in the system prefix
  records the owner's evidence on the prompt receipt
        ↓
provider
~~~

It is first so that what is current follows it and sits nearer the request: Memory, working
coordinates, the capability frame, person context, the source-return protocol or returned source, the
selected Conversation and the request itself. The preamble states that the field does not outrank
what is current.

The same projection is present in every mode: `FIRST_TURN`, `IMMEDIATE_PRIOR_COMPLETED_TURN`,
`RECENT_COMPLETED_TURNS`, `WORKING_CHECKPOINT_SUCCESSOR`, `SOURCE_RETURN` and
`SOURCE_RETURN_WORKING_CHECKPOINT_SUCCESSOR`. A turn fails before its final inference if the evidence
of the final prompt differs from the evidence of the prompt that asked for source.

It is not placed inside the WorkingSet, Memory, Person Context or the capability frame, and the
working-continuity packet does not name it.

A private conversation is given the same kernel. Private conversations keep no receipt by design.

## Room

~~~text
KernelTokenBudget = 3,600 estimated tokens
~~~

A body whose whole kernel is larger than that is invalid and is never supplied. Within a prompt the
field takes what remains after the current request, the other fixed context and the exact immediate
prior exchange an ordinary continuation must keep, so carrying it cannot fail a Send that would
otherwise fit. Its room is measured as though the source-return protocol were present in both
inferences, so the two prompts of one turn receive the same projection.

~~~text
whole kernel fits            -> CARRIED
only a priority prefix fits  -> CARRIED_PARTIAL, later parts omitted by name with TOKEN_BUDGET
no part fits                 -> HELD / TOKEN_BUDGET, a three-line notice when that fits
~~~

Parts are listed in carry priority; the last listed is omitted first.

## Currentness and failure

The field is current when the body decodes, validates, and is the latest entry of its revision record
by version and fingerprint. The fingerprint is the SHA-256 of the body's canonical JSON, so file layout
does not change it and any change to a ref, status or word does.

~~~text
body absent                      -> HELD / BODY_MISSING
body undecodable or invalid      -> HELD / BODY_INVALID
body changed without a revision  -> HELD / REVISION_UNRECORDED
~~~

Invalid includes a dangling source or part ref, a duplicate ref, a part with an origin and no source,
an `ACCEPTED` part that an unresolved item still names, a granted effect authority, an oversized
kernel, and text in the blatant form of a trait command. That last check is a tripwire against an
edit that turns the field into a persona script, not a proof that the wording is free of pressure.

When the field is held, the prompt carries only the notice that none is carried, the receipt records
the reason, and the Conversation continues. Nothing is substituted for it.

The owner is read again at the provider boundary. If what it projects is no longer what was
materialized, re-verification fails and no provider call is made.

## Evidence

Prompt receipt v8 carries the owner's evidence in `foundationalField`:

~~~text
state / heldReason
fieldRef / fieldVersion / bodySha256 / bodyRef
systemMessageSha256 / estimatedSystemMessageTokens / tokenBudget
carriedModules (ref, origins, standing, depthInSource)
omittedModuleRefs / omissionReasons
depthNotCarriedModuleRefs
sourceRefs / unresolvedRefs
effectAuthorityGranted = false
~~~

The body ref is also one of the turn's canonical context source refs. The receipt holds the exact
identity/standing/provenance relationships even when those metadata lines are not repeated in the
provider-facing kernel. The model-facing words remain the body at that fingerprint.

The new receipt fields are omitted when absent, so a v7 receipt decodes and re-encodes to the same
canonical form.

~~~text
RECEIVED
!= UNDERSTOOD OR AGREED
~~~

## Read side

~~~text
GET /api/v1/context
  the completed turn's durable evidence, never re-formed from present facts

GET /api/v1/foundational-field
  a fresh no-effect read through the same owner:
  the exact kernel text, every part with its depth, sources, unresolved items, revisions
~~~

Settings / Context adds one sentence from the turn's evidence: which version was carried, how many
parts, how many were left out, and about how many tokens; or that it was not carried and why. The
presentation reads these projections and owns none of the field.

## Revising the body

1. Edit the body and raise `version`.
2. Add an entry to the revision record with the new version, the new `bodySha256`, who recorded it,
   a summary and why.
3. `VexLifeFoundationalFieldTest` fails until the two agree, and names the fingerprint to record.

Earlier receipts keep the version and fingerprint they were given. A changed body never rewrites what
an earlier turn received.

One sentence of the reach part states another owner's truth: that no Atlas or Process Factory handle
is registered in this Product. A test holds that sentence against the capability registry, so
registering such a capability fails until the field is revised.

## Proved in source

- the shipped body is valid and is its latest recorded revision;
- the kernel is the body's own first-person paragraphs in order; depth, per-module provenance,
  standing/origin metadata and unresolved notes are not repeated into the provider-facing text while
  remaining exact in body/evidence;
- a changed word is a different body and is held until recorded;
- missing, undecodable, dangling, contradictory, authority-granting, oversized and commanding bodies
  are held with their reason;
- omission under token pressure is by name and priority, then held;
- the field is first in the system prefix in every mode and does not change the capability frame;
- one long Conversation whose early exchange is outside the window: the first inference asks for it,
  the exact pair is returned, the final prompt carries the same field, one answer becomes
  Conversation, and a second request in the same turn is refused;
- a working-checkpoint successor, alone and with source return, carries the whole field;
- a field that changes before the provider boundary, or between the two inferences, fails the Send;
- the completed turn's durable context, `/api/v1/context`, and a recovered duplicate report the same
  evidence with no new model call.

A scripted provider proves what is sent and recorded. It does not prove how a running Vex reads it.

## Held

~~~text
a lived reading of the field by a running Vex
acceptance of any part by Victor
the verbatim Victor Pattern exchanges (the body rests on the published reader notes)
a way for Vex to pull depth into a turn
whether a person's own Home should shape the field
translation: the field is English, while the reply language is a separate Person Context fact
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][FOUNDATIONAL-FIELD] -->
