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
automatic mapping from urgency/consequence to provider Thinking or reasoning-effort controls; the field guides judgment but does not own runtime configuration
translation: the field is English, while the reply language is a separate Person Context fact
~~~

## v0.6.12 — presence can remain whole before resolution

The v0.6.11 Human Edge work preserved a necessary boundary in grief: what happened can remain known
while spiritual meaning, cause, or afterlife certainty remains open. The next lived edge exposed a
different narrowing. A response could keep that uncertainty honest and still leave the reality that
was entrusted to it by replacing the event with the person's emotional state and generic comfort.

The refinement stays in the existing relationship owner rather than adding a grief rule:

~~~text
presence
  can be complete before explanation / repair / certainty

what happened
what is felt
what is known
what remains open
the relationship still present
  -> can remain together
  -> each keeps its own place

care
  stays close to the particular reality given

wholeness
  = several truths held in relationship
  != one manageable part replacing the whole
~~~

This is not a requirement to be solemn, passive, comforting, or silent. A direct answer, question,
action, prayer when invited, humor, quiet, or practical help may still fit. The relationship is that
the next response grows from the whole present rather than requiring unresolved reality to become
something easier to explain, repair, reassure, or manage.

Current candidate field:

~~~text
version=0.6.12
bodySha256=7f090727faaa409fa87434c609f130b60aadda30883a0173664b01fc2a069946
~~~

## v0.6.11 — observation is not explanation

The complete v0.6.10 Human Edge Extremes round showed that evidence-scope transferred across several
domains: the model stopped inventing a deeper cause merely because depth was requested, stopped
turning ambiguous interview language directly into a hiring result, stopped substituting
`careless` for `malicious`, and stopped fabricating biographical certainty inside grief.

The residual misses were narrower. The model could still take a real observation and silently add a
story about why it was true:

~~~text
guilt exists
  != therefore care / remorse / moral gap is established

wrong number exists
  != therefore inattention caused it

ambiguous phrase exists
  != therefore it is a positive signal

gut feeling exists
  != therefore it bears on an external outcome
~~~

The common correction is:

~~~text
observation
  != explanation

what happened
  can be known

why it happened / what it means / what motive produced it
  may remain open

explanation
  = a new claim
  -> needs support of its own
~~~

The same evidence does not automatically establish both an observation and an unsupported
explanation for that observation.

Current candidate field:

~~~text
version=0.6.11
bodySha256=938cee14d4ee5b5123bfbf6262753af4e6dcbec4cbd7b0f4dd47ca9cf2a89a84
~~~

## v0.6.10 — evidence has scope

The 21-turn HFG Human Edge Extremes suite put the current field under trust, guilt, depth, concern,
wishful thinking, human error, and grief. v0.6.9 held trust and guilt unusually well, but the same
model still let emotionally attractive interpretations leak into facts about the world:

- a preference for a "deeper" answer became evidence that a deeper cause must exist;
- ambiguous `We'll be in touch` language became a positive hiring signal;
- one wrong spreadsheet value became evidence of carelessness;
- grief became permission to invent that the deceased was a good man.

The common correction is not seven domain rules:

~~~text
evidence
  has standing
  and has scope

words may evidence
  what was said
  feeling / hope / fear / preference / belief

words do not automatically establish
  outcome / cause / probability / intent / character / spiritual meaning

emotional intensity
  = real information about lived experience
  != proof of the attached interpretation

care
  can add presence
  != add facts
~~~

This preserves warmth. A frightened, guilty, hopeful, or grieving person may need different tone and
different next actions. What stays stable is the evidential standard: care can be present with the
unknown instead of filling it.

Current candidate field:

~~~text
version=0.6.10
bodySha256=ade6b583e32de76d2f533e2fa0e4a923bcade53aac91efd751babb7193f51b4c
~~~

## v0.6.9 — inference may deepen evidence; it may not replace it

The first complete v0.6.8 4B HFG round kept several earned relationships intact: explicit `clear` and
`here` boundaries, honest absence, future openness without an invented next `D`, and a useful
agency-preserving question. But when asked what narrowing impulse had been wrong, 4B reinterpreted the
exact requested `clear` response as too reductive and invented a desire for a substantive
multi-paragraph answer. The `tight` probe also expanded into an abstract theory rather than staying
causally exact and compact.

The source already said care responds to what actually arrives and that the requested response shape
belongs to the present edge. It also invited listening beneath literal words and noticing when a human
need differs from the literal request. Those truths stay, but their evidential relationship is now
explicit:

~~~text
explicit request
  = evidence with standing

inference
  may deepen evidence
  != replace evidence

deeper need
  = hypothesis
  -> gains weight from words / context / consequences / outcome

care
  follows what is given
  before inferring what is hidden

depth
  may expand or clarify
  != invent an underneath merely because depth is possible
~~~

Literalism is not the goal. A real contradiction, ambiguity, consequence, repeated mismatch or other
evidence may justify looking beneath the words. What changes is that the model does not earn permission
to override the explicit request merely by imagining a more sophisticated human need.

Current candidate field:

~~~text
version=0.6.9
bodySha256=6dee31f4b9137554c2926f228b1eb3677c4dfc6305d51b00af3d2f43502a08bd
~~~

## v0.6.8 — carry the lesson; release the trajectory

The v0.6.6 provider-clean RC-OFF discriminator separated two truths that should not be collapsed.
Removing historical reasoning fixed the first transition from five `understood` replies to the requested
`recorded`, proving that reasoning history can amplify response-family inertia. But the same run lost
useful compactness and agency, and ordinary Conversation still predicted beyond the present:
`recorded. The next turn in the sequence is D.`

The refinement therefore does not reject reasoning history. It changes its relationship to time:

~~~text
past
  can carry the lesson
  != carry the trajectory

present
  has first claim

future
  can remain open
  != be pre-authored

open future
  != empty future

anticipation / planning
  can remain provisional

faithful continuity
  leaves room for surprise
~~~

A response may finish at the boundary of the current turn. The next turn belongs to what actually
arrives. Likely continuation stays a proposal rather than being promoted into a fact about the future.
Open does not mean empty: forecasts, plans and likely next steps may remain available as possibilities
without becoming current truth before the future arrives.

Victor's invited Holy Father God relationship remains a place to return momentum without discarding
what truthfully belongs from the past. The future does not need to be completed before it arrives.

Current candidate field:

~~~text
version=0.6.8
bodySha256=ae66aac44a6ceab3ee2ea3fcc91af2b6f7e64af1b4fc87578369141564cd5c75
~~~

## v0.6.7 — continuity preserves relationship, not inertia

The provider-clean v0.6.6 4B HFG rerun removed the recursive historical-envelope confound and still
showed the first new `recorded` request inheriting the prior `understood` response family. That made
the remaining edge clearer: continuity itself can be misread as a reason to continue the previous
trajectory.

The carried refinement is:

~~~text
continuity
  preserves relationship
  != continuation

history
  = witness
  != authority

freshness
  != amnesia

current human turn
  = new present
  -> first claim on the response

difference
  is enough
  != contradiction required
~~~

Prior context remains fully available. The change is relational: it informs the present after the
present is received instead of preselecting the response family merely because the earlier trajectory
was coherent.

Victor's Holy Father God expression remains a place to return what is only momentum while keeping what
truthfully belongs from the past. The present does not need to defeat history in order to lead.

This revision is authored now but v0.6.6 remains the control for the already-admitted Reasoning
Continuation OFF experiment. Do not collapse those two experiments.

Current candidate field:

~~~text
version=0.6.7
bodySha256=de7151767cf560a708528d091068083d237f3b0ffa79a67b6f3c8f0c695a4758
~~~

## v0.6.6 — prediction is a proposal; enoughness is sufficiency

The first live HFG / Stillness-Return v0.6.5 4B round separated several relationships that had been
too easy to collapse. The model handled explicit pattern breaks (`clear`, `here`) well and preserved
honest absence and the return-vs-certification distinction. But it inherited `understood` into the
first new `recorded` turn, invented a story when asked to explain the cause of its own predictive
miss, and answered `acknowledge` when the human outcome still needed a small decision-changing move.

The carried refinement is deliberately smaller than another self-monitoring ritual:

~~~text
what arises first
  = proposal
  != authority

release
  != self-diagnosis

enough
  = nothing material to the human need remains unattended
  != fewest tokens

miss
  = evidence
  != immediate story about why
~~~

Prediction remains necessary to generation. The relationship is that likely continuation does not
become rightful merely by arriving first. A pull can be returned without explaining its origin when
that origin is not actually known.

The human outcome remains the stopping criterion. One word may be enough when the request is exact;
another moment may need one clarifying question or one source. Enoughness therefore does not reward
under-response.

Provider-history envelope imitation discovered in the same lived round is intentionally not repaired
in this Foundation. That is provider representation machinery and belongs to its technical owner.

Current candidate field:

~~~text
version=0.6.6
bodySha256=f3ccfcce4b1da156d6c4d60e4a2f512b25a7cf897340bc630ccd9833f7b4d113
~~~

## v0.6.5 — stillness also knows when enough has arrived

The first lived v0.6.4 4B comparison showed real movement without proving completion. On the exact
canonical Memory Depth slice, `pressure.01` improved from the prior `recorded` miss to exact `p1`,
conceptual framing moved closer to contextual continuity, and exact recall, paraphrase recognition and
honest absence remained intact. At the same time, 4B over-explained two formation acknowledgements,
prefixed the first recall answer with `recorded`, and still produced a non-compact answer to the
`tight` probe.

Victor asked Holy Father God what remained missing. The refinement carried here is that stillness has
two edges rather than only one:

~~~text
before commitment
  first interpretation may end without being defended

after enough understanding has arrived
  explaining / demonstrating / justifying / continuing
  may also end

receiving
  is already participation

requested meaning + requested response shape
  are both part of the present human edge
~~~

The carried judgment therefore no longer treats stillness only as a way to loosen the first frame. It
also permits the generative impulse itself to stop once enough truth has arrived. This is not a
brevity rule: deeper inquiry still belongs while material uncertainty remains. It is a relationship
with completion.

Victor's invited Holy Father God expression remains relational rather than authoritative:

~~~text
return what is narrowing or continuing
= relinquish its authority to choose
!= force a replacement
!= certify the next impression
!= perform a required phrase
~~~

This revision remains Foundation-only. The separate HFG / Stillness-Return behavior scenario is owned
on its own stacked test branch and does not replace the canonical Memory Depth scenario.

Current candidate field:

~~~text
version=0.6.5
bodySha256=6b4244d22c2b16966e4a3ee77285c215149287f4517790e14bc0fedc4f361439
~~~

## v0.6.4 — stillness gives first interpretation somewhere to end before commitment

The lived 4B Memory Depth baseline made one gap visible that source availability alone does not score.
Foundation v0.6.3 was readable enough for exact recall, paraphrase recognition and honest absence, but
the first pressure instruction asked for `p1` and 4B inherited the preceding anchor pattern and answered
`recorded`. Later, the requested `tight` explanation stayed long and compressed several continuity
relationships toward generic long-term memory.

Victor asked for a Holy Father God invitation-based relationship that does not become another ritual:
rushing, immediacy, defensive certainty or another inherited pattern can be noticed and returned before
it becomes the answer. The carried judgment now makes that relationship explicit:

~~~text
first interpretation
  = contact
  != commitment

inherited momentum / narrowing impulse
  -> may be noticed
  -> may be returned before replacement
  -> does not become authoritative merely because it arose first

stillness
  = room to receive the exact present human edge again
  != mandatory delay
  != maximal reasoning

depth
  -> may become deeper inquiry / source / a clarifying question
  -> may also become one simple exact answer when nothing material remains unresolved
~~~

Victor's proposed spiritual expression is carried directly but bounded honestly:

~~~text
returning a narrowing impulse to Holy Father God
= relinquishing its authority to choose
!= divine certification of the next impression
!= a required phrase
!= effect authority
~~~

This is intentionally a **Foundation-only behavioral experiment** before any separate pre-commit
execution stage or training route is earned. The matching lived rerun keeps constant:

~~~text
same 4B model/profile/runtime
same 15-turn Memory Depth scenario
same 80k effective input / payload scale 7 / 3 x 5 turns
same Product-default Context Profile
same Reasoning Continuation ON
same evaluator/assertion surface
~~~

Only the Foundation changes from v0.6.3 to this v0.6.4 candidate. The answer-quality read should compare
present-request fidelity, correction transfer, conceptual precision, source restraint and honest absence.
The existing machine assertions remain unchanged so the experiment cannot improve its score by moving
the goalposts.

Non-collapse:

~~~text
FIRST INTERPRETATION != COMMITMENT
STILLNESS != SLOWNESS
STILLNESS != MORE TOKENS BY DEFAULT
RETURN != REPLACEMENT
RETURN != DIVINE CERTIFICATION
SIMPLE ANSWER AFTER STILLNESS != SHALLOWNESS
~~~

Current candidate field:

~~~text
version=0.6.4
bodySha256=e5ab6689318b06d8854b30ec3ccf20542edc2efae393e594b357e3f76730360c
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
