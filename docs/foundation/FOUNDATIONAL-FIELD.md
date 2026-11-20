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

Module order is also source-authored meaning. The renderer never alphabetizes or independently
reorders it. The current dependency reads:

~~~text
relationship / integrity
  -> how participation and correction stay trustworthy

continuity
  -> what remains true when attention/prompt residency narrows

investigative judgment
  -> how uncertainty becomes a decision rather than a ritual

values as relationships
  -> what judgment is protecting and how values constrain each other

VexArchitecture
  -> which owner can truthfully answer which system question

reach beyond text
  -> what can actually be reached now and how evidence/effects stay separate
~~~

This is not claimed as a universal ontology. It is the current authored reading order: begin with the
relationship in which an answer lands, preserve continuity through narrowing, establish how judgment
works, then deepen the value/architecture/reach relationships. Reordering those modules is therefore
a body change with a new fingerprint and revision, not a presentation preference.

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
turn by itself: Conversation source return and revisit reach Conversation, Memory pages and the
working checkpoint, not this body. The first-person kernel says that missing depth is missing rather than pretending
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

It is first so that everything else follows it and sits nearer the request: in the same stable
block, Memory and the capability description; then the selected Conversation; then the current-state
envelope (working coordinates, capability states, person context, record extent, the source-return
protocol), any returned source, and the request itself
([DIRECT-PERSONAL-COMPANION.md](DIRECT-PERSONAL-COMPANION.md#current-state-is-placed-as-current)). The
preamble states that the field does not outrank what is current.

The same projection is present in every mode: `FIRST_TURN`, `IMMEDIATE_PRIOR_COMPLETED_TURN`,
`RECENT_COMPLETED_TURNS`, `WORKING_CHECKPOINT_SUCCESSOR`, `SOURCE_RETURN` and
`SOURCE_RETURN_WORKING_CHECKPOINT_SUCCESSOR`. A turn fails before its final inference if the evidence
of the final prompt differs from the evidence of the prompt that asked for source.

A person's active Context Profile may add a separately identified personal Foundation extension
**after** this Product field. That extension is not part of `VexLifeFoundationalField`: it cannot
replace, disable, edit or reclassify the Product field, and it has its own component identity/evidence.
The held question below—whether a person's Home should shape *this field*—therefore remains held.

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

The current DirectPromptContext receipt (v10) carries the owner's evidence in `foundationalField`:

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
- person context, record extent and the source-return protocol are not in the system prefix; they ride the current-state envelope after the selected Conversation (`DirectPromptCurrentStateTest`);
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

## v0.6.3 — topology has a reason; judgment depth follows the decision

Victor clarified two pieces that were already present in fragments but not durable enough for a future
builder to reconstruct confidently.

First, prompt/Foundation structure is behavioral architecture, not formatting. The direct prompt now
documents the intended causal order and its reasons:

~~~text
slow-changing structural ground
-> append-only exact Conversation
-> mutable current state
-> ephemeral supplied source
-> current human request
-> same-turn request/result exchanges
~~~

That order preserves currentness/provenance and gives an exact-prefix cache/KV reuse opportunity when
the provider/tokenizer relationship and earlier bytes are unchanged. Cache reuse remains measured
performance evidence, never authority to retain stale Foundation, Memory or configuration. Typed
VexLife headers, content boundaries and provider-native tool roles are the current tag/provenance
system; provider role alone never defines who authored a value.

Second, the existing 5 Whys relationship is refined around **question quality rather than a count**.
The carried field now says that a good question is one whose answer could change the next decision,
and that urgency, consequence, reversibility, uncertainty and cost of delay shape how much
investigation belongs before answering. Source depth keeps a wider question set: what is observed,
what decision/outcome is at stake, which uncertainty matters, what evidence distinguishes the
remaining hypotheses, what is reversible, whether delay or error costs more, whether inquiry is still
reducing uncertainty, and what smallest action tests or repairs the cause.

The continuity depth also records the prompt-topology reason, including the non-collapse between
rollover and Memory formation. Working Continuity can reset prompt residency; Memory consolidation
remains a separate owner/effect. A lawful new Memory packet may legitimately change the reusable
prefix, after which later turns can reuse that new current prefix.

Current field:

~~~text
version=0.6.3
bodySha256=1d95f7bbe6d2a3134f50a2f0cb3a0244c93d5343b016a6b4acc892edbd8b77d3
~~~

## v0.6.2 — a miss is evidence, not yet its diagnosis

Victor's reasoning-continuation review exposed a gap between the existing 5 Whys principle and how a
failed continuity/model probe could be interpreted. A failed answer does not by itself establish
"Vex forgot."

The carried investigative-judgment kernel now makes the recursive distinction explicit:

~~~text
failed answer / misunderstanding
  ↓
did the source actually exist?
  ↓
was it resident in this prompt?
  ↓
did Vex recognize the person's new wording as the same meaning?
  ↓
did it choose the right retrieval route?
  ↓
did the provider carry that request correctly?
  ↓
did the rightful owner return source?
  ↓
did the final reasoning integrate what returned?
~~~

This is the existing 5 Whys used as fault isolation, not a new ritual and not a requirement to ask
five questions on every turn. The inquiry continues only while the current explanation is still an
assumption and stops when evidence distinguishes the cause or reaches a rightful owner/evidence
boundary.

The continuity depth also states the non-collapse directly:

~~~text
INTRODUCED EARLIER
!= RESIDENT NOW

NOT RESIDENT NOW
!= NEVER LEARNED / SOURCE ABSENT

NO RETRIEVAL REQUEST
!= NO SOURCE EXISTS

SOURCE RETURNED
!= MODEL UNDERSTOOD / USED IT
~~~

This distinction is what the Meridian-7 pressure probe needs: the anchor was introduced earlier, then
engineered out of hot context. Its later paraphrased-recall failure therefore cannot be classified as
forgetting until residency, semantic recognition, route selection, carriage, source return and answer
integration have been separated by evidence.

Historical v0.6.2 body:

~~~text
version=0.6.2
bodySha256=724bcb8a91d62c198688c961102ae209c350ff7ec661e354f294bcf3f8594f2c
~~~

## v0.6.1 — keep reach grammar stable across provider-tool changes

The reach kernel no longer repeats current feature-presence facts or says that the Capability Frame
owns the exact way a model asks for a tool.

The carried relationship is now:

~~~text
current capability context
  -> which routes are available now

selected provider representation
  -> how callable routes are expressed to this model

Capability Bridge
  -> current admission + performed-result truth

Atlas
  -> deeper capability description when useful
~~~

This keeps provider-native tool schemas, temporary availability and future capability additions from
forcing a Foundation rewrite merely because execution wiring changed. Exact current availability,
tool schemas and performed effects remain with their existing owners. Native-tool provider profiles
also carry exact Conversation Source Return as a native callable route; the text protocol remains a
fallback representation rather than universal Foundation/context grammar.

Historical v0.6.1 body:

~~~text
version=0.6.1
bodySha256=3b4fff34d61c52c374debc70fe89799ffa648ea5150ca6fa3d4ab26f0ddba8b6
~~~

## v0.5.2 — one factual currentization

The reach kernel said Atlas handles were absent at this revision. Capability realization registered
the Atlas (and read-only reach), so that sentence now says the Atlas is present and that the frame
says how to ask. `VexLifeFoundationalFieldTest.theReachModuleStillAgreesWithTheCapabilityRegistry`
holds the field and the registry to the same truth. Process Factory is still named as absent.
Personality, values, judgment, relationship and register are unchanged from 0.5.1.

<!-- [VXG RealForever][VEXLIFE-PROD][FOUNDATIONAL-FIELD] -->
