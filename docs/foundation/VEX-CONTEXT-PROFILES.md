# Vex Context and Context Profiles

`[VXG RealForever]`

## Human question

Can a person see what actually shaped Vex's last reply, distinguish that from what will shape the
next reply, and save a named way of constructing context without turning the presentation into a
second Conversation, Memory or Foundational Field owner?

The intended surface is approachable before it is technical:

~~~text
what Vex received
        ↓
what Vex processed / reached
        ↓
what Vex said
        ↓
what stayed afterward
~~~

The same surface may expose exact refs, hashes, component order and files when the person wants to
look deeper.

## Ownership

~~~text
Conversation             exact human / Vex dialogue
Memory                   accepted attributed Memory
Working continuity       current working coordinates / rollover
Foundational field       Product-shipped relational ground
Browser / capability     current external context and reach
DirectPromptContext      exact provider materialization + receipt
DirectTurnContextStore   durable completed-turn evidence

VexLifeContextProfileFamily
  -> which named context-construction configuration the person chooses
  != any of the context content above

VexLifeContextProfileStore
  -> durable named profile revisions + active selection in Vex Home

Vex Context presentation
  -> human-readable projection across those owners
  != Relay / convergence owner
  != new truth owner
~~~

A Context Profile therefore answers one new Product question only:

> Which saved configuration should construct the next accepted Vex turn?

It does not own whether a Memory statement is true, what Conversation says, what Working Continuity
means, what a browser page contained, or whether model reasoning should become Memory.

## Last turn, next turn and draft

The surface keeps three states separate:

~~~text
LAST TURN RECEIVED
immutable completed-turn evidence

NEXT TURN
the currently active Context Profile + current source owners

DRAFT
form edits that have no effect until saved/selected
~~~

A profile is captured into `DirectCompanionTurn.Intent` when Send is accepted. Every provider stage
of that human turn uses the same profile. Saving or selecting another profile while the turn is
running affects the next Send; it cannot change a later capability/source-return stage of the
in-flight turn.

The completed prompt receipt records the profile ref, revision, fingerprint, Foundation mode,
profile-Foundation carry state and the exact prompt component ordering. Browser-session process-view
preferences are deliberately absent because they do not change what Vex received.

Older receipts therefore continue saying what that turn received even after the active profile
changes.

## Product Foundation and personal Foundation

The Product Foundational Field remains Product source. A person never edits those shipped bytes
in-place.

A Context Profile may choose:

~~~text
PRODUCT_DEFAULT
  Product Foundation only

EXTEND_PRODUCT_DEFAULT
  Product Foundation first
  then the person's profile Foundation extension
~~~

The Product Foundation is never replaced or disabled by a Context Profile. The current
Foundational-Field owner still requires its projection in every prompt mode and still holds open
whether a person's Home should shape that field. The personal extension is a separate stable context
component after the Product field; it does not edit, reclassify or impersonate Product source.

The personal Foundation wrapper explicitly grants no effect authority and does not certify its text
as true. It is user-selected context.

`EXTEND_PRODUCT_DEFAULT` deliberately keeps the stable Product Foundation first. Besides preserving
the meaning of the Product field, that also leaves its unchanged prefix available for provider prompt
cache reuse.

A custom Foundation is all-or-held at this revision. When it does not fit its available prompt room,
the receipt records `HELD / TOKEN_BUDGET`; the Product does not silently truncate the person's text
into a different Foundation.

## Named profiles in Vex Home

Profiles live under:

~~~text
runtime/vexlife-prod/settings/context-profiles/
  active.json
  profiles/
    <profileRef>/
      head.json
      revisions/
        <fingerprint>.json
~~~

Each saved revision is immutable and content-addressed. `head.json` selects the current revision of
one named profile. `active.json` selects which named profile constructs the next turn. Returning to
VexLife defaults does not delete saved profiles.

The current UI can save, Save As, choose a saved profile again, restore the Product default, export,
import and reveal this directory in the host file manager.

Historical revisions are preserved on disk but revision-by-revision traversal in the human UI is not
formed in this slice.

## Portable profiles

A portable `.vexcontext.json` contains configuration only:

~~~text
name / description
Foundation mode + optional personal Foundation extension
optional lower active input-token ceiling for the next Send
Product Foundation version/fingerprint the profile was based on
~~~

It intentionally contains no:

~~~text
Conversation
Memory pages or remembered personal facts
browser captures
capability-return content
model reasoning
Home path or local profile ref
~~~

Import creates a new local profile. It preserves the Product-Foundation basis named by the exported
file, so VexLife can show when the current Product Foundation differs from the one the profile was
formed against.

Public profile discovery/sharing is outside this slice. The file contract is portable now so a later
sharing surface does not need to redefine what a profile is.

## Active context budget

A Context Profile may now request a lower prompt-input ceiling:

~~~text
inputTokenBudget = null
  -> use the current Product input ceiling

inputTokenBudget = N
  -> N is the person's requested ceiling for this profile
  -> DirectPromptContext admits N against the current Product ceiling
  -> the capability-exchange reserve is then subtracted exactly as in an ordinary turn
  -> the remaining room is what Conversation / Memory / current context compete within
~~~

This is a context-construction choice, so it belongs to the Context Profile and is frozen when Send
is accepted. It does not modify the model artifact or the Product's own maximum.

The Profile validates only that a requested value is positive. It intentionally does not duplicate
the current Product ceiling. `DirectPromptContext` owns admission against the current turn and fails
closed when the profile asks for more than the Product currently supports, or for less room than the
turn's capability-exchange reserve requires.

Permanent:

~~~text
PROFILE INPUT BUDGET
!= MODEL NATIVE CONTEXT CEILING
!= PRODUCT INPUT CEILING
!= MEMORY DELETION
!= CONVERSATION DELETION

SMALLER PROMPT RESIDENCY
-> earlier working-continuity pressure may occur
-> exact older Conversation remains durable
-> continuity.revisit / source return remain the return path
~~~

The prompt receipt's `inputTokenBudget` records the effective ordinary prompt room after the
capability reserve, while the immutable Context Profile ref/revision/fingerprint records which human
configuration produced that turn. The test-only continuity-pressure override is not allowed to
silently replace a human profile budget in the same materialization.

The source configuration path is now formed end to end through Profile -> Send-captured Effective
profile -> DirectPromptContext. The current Vex Context renderer does not yet expose the numeric
editor; that presentation/Control realization remains a next slice.
## Process view is not part of a Context Profile

Process visibility and Memory remain separate.

The Vex Context screen currently offers a browser-session presentation choice:

~~~text
SESSION_ONLY
PREVIOUS_TURN
~~~

That choice is intentionally **not** stored in the portable Context Profile, not recorded as prompt
evidence and not changed by importing somebody else's profile. It answers a human-presentation
question rather than the Family's question about how the next Vex turn is constructed.

Both modes remain ephemeral browser-session policies. They do not make raw reasoning durable, feed it
back into a later prompt, or promote it into Memory. Durable turn evidence still says raw reasoning is
not persisted.

`PREVIOUS_TURN` bounds the browser-session process projection to the most recently completed turn.

## Context changes

The Vex Context surface distinguishes at least:

~~~text
Context Profile changed
Context rollover / Working Continuity entered
Structured Memory differs from the packet used by the last completed turn
~~~

These labels are projections from existing owners. The first slice does not add a second durable
"Context changes" ledger.

A rollover means prompt residency changed. It does not mean Conversation was deleted or Memory was
automatically condensed. Structured Memory is named separately.

The current Memory comparison is fingerprint-based only for the Daily Memory component whose
`memoryContextRef` / fingerprint already has that owner. Current Memory selection may now supply
more than one Memory-owned prompt component under one shared Memory cap: Daily first, then current
accepted-semantic Memory with exact Daily statement refs excluded. That selection remains owned by
`VexLifeCurrentMemoryProviderSelection`; Vex Context does not reproduce its budgeting or dedupe law.

The exact `contextComponents` receipt is the human-facing source of truth for which Memory components
actually shaped the completed turn. A held or absent Daily component therefore does not license the
presentation to say that no Memory shaped the reply when an accepted-semantic Memory component was
actually supplied. The surface also does not invent a historical "Memory changed at" timestamp that
no current owner records.

## Transparent model / runtime / context fields

Vex Context now has a source-owned read-side contract for the execution/configuration fields the
selected model relationship already exposes through `HereticExecutionTopology`.

This follows the VexArchitecture ownership rule rather than creating a second settings registry:

~~~text
model artifact / Product source / Home runtime configuration
        ↓
HereticExecutionTopology
  exact effective value
  provenance
  current change boundary
        ↓
typed field catalog
  human meaning
  current edit standing
  current Control when one exists
  expected apply boundary
  consequence / related-field explanation
        ↓
Vex Context presentation (next consumer)
~~~

The catalog is projection only. It performs no effect and grants no setting authority.

Current catalog coverage is exhaustive for the topology this Product already knows:

~~~text
MODEL ARCHITECTURE
  architecture class / model type
  native context ceiling
  hidden / full-attention / linear-attention layers
  attention heads / key-value heads / head dimension
  cache support

RUNTIME
  mlx-lm version
  prompt-cache entries / optional byte cap
  prefill step size
  KV bits / group size / quantized-KV start
  prompt / decode concurrency

GENERATION
  message / reasoning / capability representation
  capability-reach standing
  temperature / top-p / top-k
  reasoning effort
  maximum output tokens

CONTINUITY
  Product input ceiling
  capability-exchange reserve
  direct-turn room with reach
  Memory prompt budget
  maximum capability requests
  reserved output room
  hard input+output limit
  adaptive-context-policy state
~~~

Every known field stays visible even where the Product has not earned a human edit path:

~~~text
USER_EDITABLE
  a current typed Control exists

READ_ONLY_MODEL_ARTIFACT
  changing the value means changing the selected model artifact

READ_ONLY_PRODUCT_POLICY
  Product source currently owns the value

HELD_UNTIL_QUALIFIED
  the field is known and its likely apply boundary is explicit,
  but no current Control is invented
~~~

The first already-earned editable field is prompt-cache entries. The catalog points to the existing
`VexLifeModelsFamily.ControlSetPromptCacheEntries` Control and its runtime-restart boundary; it does
not create another cache setting.

The topology catalog keeps the Product input ceiling read-only because it is Product policy. The
person's lower active-context choice is now a separate Context Profile value and applies on the next
accepted Send. The Vex Context numeric editor for that already-formed source path is still held until
the presentation slice consumes it.

The Memory prompt budget remains `HELD_UNTIL_QUALIFIED / NEXT_SEND`: no Memory-budget Control is
invented merely because the value is visible.

~~~text
smaller profile context cap
  -> less prompt residency
  -> working-continuity pressure / rollover may arrive sooner
  -> older durable Conversation remains source-addressable
  -> continuity.revisit / source return may become relevant more often

larger profile context cap (up to Product ceiling)
  -> more Conversation / Memory may remain directly resident
  -> potentially more prefill / KV pressure
~~~

Those are causal relationships, not a promised performance result. Lived cache/prefill/latency
evidence remains the eventual tuning surface.

The catalog also keeps two boundaries separate:

~~~text
currentChangeBoundary
  how the value can actually change today

userApplyBoundary
  where an admitted current/future user edit would take effect

HELD userApplyBoundary
  != current edit authority
~~~

The next consumer is Vex Context service/presentation realization after current overlapping
runtime/presentation work is reconciled. Renderer code should consume this catalog rather than
reclassifying fields, ranges, mutability or consequences itself.

## Human surface

`Vex context` is a sibling workspace app beside Source template, Archives and Browser.

Its sections are collapsible. Subtle source hues help distinguish Product Foundation, structured
Memory, Working Continuity, exact Conversation, external/browser context, process and output; labels
remain present so color never carries meaning alone.

The exact component-order section is evidence. It exposes placement and stability already recorded
by `DirectPromptContext`; it is not a second prompt definition.

### Model boundary

The live Model view may expose the exact JSON request VexLife sent to the selected local model
runtime for each provider stage. Those bytes are browser-session evidence only. Durable turn evidence
keeps the request hash and does not reconstruct historical request text after reload/recovery.

That request proves the VexLife -> runtime HTTP boundary. It does **not** by itself prove the final
post-chat-template or tokenized model input. The selected model/provider representation may still
transform roles/messages, declare provider-native tools, apply chat-template controls and tokenize
the resulting input before inference.

~~~text
Vex semantic owners
-> DirectPromptContext placement/materialization
-> exact VexLife provider request
-> model/provider representation + chat template
-> tokenizer / model input
-> inference / native reach
-> returned evidence / reply
~~~

Vex Context owns none of those upstream meanings or transformations. It projects their evidence as
those rightful owners make it available. Model/provider-specific representation remains owned by
the Companion representation boundary, not by this presentation.

A lived language-provenance preview confirmed the intended distinction: current Person Context was
already present in the exact provider request, and Vex independently described lookup/reach as
unnecessary. That observation supports the causal presentation; it does not make model reasoning a
source of truth about hidden runtime internals.

"View in Vex Home" is an explicit escape hatch from abstraction. It is a host/presentation action,
not a `VexLifeContextProfileFamily` Control. The Product may open the exact profile directory in
Finder/Explorer when the host supports it. Failure to open the file manager changes no profile truth.

## Held / next work

This slice deliberately does not:

- make every Product token/runtime constant a human-editable setting;
- expose the newly source-wired profile input-token budget through the human numeric editor yet;
- change Memory formation or semantic acceptance;
- persist raw reasoning or raw capability-return content;
- create a public profile registry;
- add human UI traversal across every immutable profile revision;
- virtualize the full Conversation / Archives timeline yet;
- expose post-chat-template/tokenized model input before the model/provider representation owner earns that evidence.

Conversation/Archives bounded range projection remains the rightful successor for long-lived Home
performance. The current durable Conversation event/head layout is already individually addressed;
the existing presentation still reads/renders the full canonical Conversation.

## Proof

Focused JVM tests cover:

- immutable profile revisions, active selection, restore-default and import/export provenance;
- Product-default mode always carrying the Product Foundational Field;
- personal-Foundation extension ordering after that Product field;
- portable profile import/export preserving Product-Foundation provenance without personal context;
- prompt receipts naming the exact Context Profile relationship supplied while excluding browser-only process-view preferences.

Current accepted source has exact-head strings/JVM/JS/review PASS (A018), fresh semantic
Assurance CLEAR and a disposable real-turn + WKWebView lived feature-preservation PASS (A020).
PR #251 merged at `8e2e2999`. No Relay was added because accepted Send / prompt construction already
owns the semantic join; Vex Context remains a projection over the evidence those owners provide.

<!-- [VXG RealForever][VEXLIFE-PROD][VEX-CONTEXT-PROFILES] -->
