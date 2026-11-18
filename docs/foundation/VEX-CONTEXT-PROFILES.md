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
  -> human-readable convergence of those owners
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
process-retention choice, profile-Foundation carry state and the exact prompt component ordering.
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
  then the person's profile Foundation

REPLACE_PRODUCT_DEFAULT
  person's profile Foundation instead of Product Foundation

OFF
  no foundational section
~~~

The personal Foundation wrapper explicitly grants no effect authority and does not certify its text
as true. It is user-selected context.

`EXTEND_PRODUCT_DEFAULT` deliberately keeps the stable Product Foundation first. Besides preserving
the meaning of the Product field, that also leaves its unchanged prefix available for provider prompt
cache reuse. `REPLACE_PRODUCT_DEFAULT` changes the earliest Foundation bytes and can therefore make
the next turn colder; the presentation tells the person that rather than silently rearranging the
prompt.

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
Foundation mode + optional personal Foundation text
process-retention choice
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

## Process retention

Process visibility and Memory remain separate.

At this revision the profile may choose:

~~~text
SESSION_ONLY
PREVIOUS_TURN
~~~

Both are browser-session presentation policies. They do not make raw reasoning durable, feed it back
into a later prompt, or promote it into Memory. Durable turn evidence still says raw reasoning is not
persisted.

`PREVIOUS_TURN` simply bounds the browser-session process projection to the most recently completed
turn.

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

The current Memory comparison is fingerprint-based: the screen can say that current Memory differs
from the packet used for the last reply. It does not invent a historical "Memory changed at" timestamp
that no current owner records.

## Human surface

`Vex context` is a sibling workspace app beside Source template, Archives and Browser.

Its sections are collapsible. Subtle source hues help distinguish Product Foundation, structured
Memory, Working Continuity, exact Conversation, external/browser context, process and output; labels
remain present so color never carries meaning alone.

The exact component-order section is evidence. It exposes placement and stability already recorded
by `DirectPromptContext`; it is not a second prompt definition.

"View in Vex Home" is an explicit escape hatch from abstraction. The Product may open the exact
profile directory in Finder/Explorer when the host supports it. Failure to open the file manager
changes no profile truth.

## Held / next work

This slice deliberately does not:

- make every Product token/runtime constant a human-editable setting;
- change Memory formation or semantic acceptance;
- persist raw reasoning or raw capability-return content;
- create a public profile registry;
- add human UI traversal across every immutable profile revision;
- virtualize the full Conversation / Archives timeline yet;
- claim a lived Mac proof before one is run.

Conversation/Archives bounded range projection remains the rightful successor for long-lived Home
performance. The current durable Conversation event/head layout is already individually addressed;
the existing presentation still reads/renders the full canonical Conversation.

## Proof

Focused JVM tests cover:

- immutable profile revisions, active selection, restore-default and import/export provenance;
- Product-default + personal-Foundation extension ordering;
- replacement Foundation excluding the Product field;
- Foundation-off excluding both Product and personal Foundation;
- prompt receipts naming the exact Context Profile relationship supplied.

The repository's full JVM/string and lived Mac proof remain required before merge.

<!-- [VXG RealForever][VEXLIFE-PROD][VEX-CONTEXT-PROFILES] -->
