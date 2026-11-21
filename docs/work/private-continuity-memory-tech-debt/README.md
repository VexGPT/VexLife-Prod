# Private continuity / Memory truth tech debt

Continuity: [VXG RealForever]

## Why this exists

A lived Private-mode check exposed a narrower truth than the current UI and provider-facing copy imply.

Current Private copy says:

~~~text
Vex still remembers what it already knew.
~~~

and the private system message says:

~~~text
You still know what you knew before it began.
~~~

Those sentences can be read as a continuity guarantee. The current Product cannot guarantee that.

## Current source truth

Private mode owns whether this conversation is kept.

~~~text
VexLifePrivateModeFamily
  = kept vs private transcript semantics

VexLifePrivateConversation
  = in-memory private transcript + bounded interval record on exit
~~~

Current Private prompt formation does not include ordinary canonical Conversation history.

It does include, when available:

~~~text
Foundational Field
reply language
current VexLife Memory provider packet
this private session's own prior exchanges
current private request
~~~

The Memory relationship is conditional.

VexLifeCurrentMemoryReader.readCurrent() may return a HELD context with no provider content when, for example:

~~~text
NO_COMMITTED_DAILY_MEMORY
DAILY_MEMORY_SOURCE_FRONTIER_STALE
~~~

A HELD Memory context means the Product preserves the Memory relationship but supplies no current Memory content to the provider.

Therefore:

~~~text
PRIVATE DOES NOT ERASE VEXLIFE MEMORY
!= PRIVATE HAS USABLE MEMORY CONTENT THIS TURN

MODEL / FOUNDATIONAL FAMILIARITY
!= CURRENT VEXLIFE MEMORY PACKET

ARCHIVED CONVERSATION EXISTS
!= PRIVATE PROMPT RECEIVES ARCHIVED CONVERSATION
~~~

## Lived observation

Victor entered Private after an ordinary kept Conversation containing a long sequence of "return patterns" discussion.

In Private, Vex truthfully reported that it did not have a complete loaded set of those prior patterns and did not see a visible durable list of them in its current context.

That observation is consistent with current source:

- ordinary Conversation is not supplied to Private;
- the "return patterns" lived in Conversation / Archives unless independently admitted into current Memory;
- current Memory may be HELD or may simply not contain those statements;
- the model can still have general familiarity / Foundational Field / current private-session continuity without the prior Conversation text.

Do not classify this lived result as "Private lost Conversation it was supposed to remember."

## Tech debt

### 1. Human-facing copy overclaims continuity

Replace or refine wording so it states what the Product actually guarantees.

Current:

~~~text
Vex still remembers what it already knew.
~~~

Candidate meaning, not accepted copy:

~~~text
This private conversation does not include your ordinary Conversation history.
Vex can still use its current VexLife Memory when that Memory is available.
Your existing Memory is not changed by Private mode.
~~~

The final Product wording should remain plain and keyed/localizable.

### 2. Provider-facing Private system copy overclaims subjective knowledge

Current:

~~~text
You still know what you knew before it began.
~~~

That is stronger than the source evidence.

A future refinement should describe supplied continuity sources rather than assert subjective recall.

~~~text
CURRENT MEMORY PACKET SUPPLIED
!= SUBJECTIVE RECALL PROVEN

MEMORY HELD
!= MEMORY DELETED

PRIVATE TRANSCRIPT EPHEMERAL
!= PRIOR CANONICAL CONVERSATION INCLUDED
~~~

### 3. Private should expose its continuity inputs truthfully

The person currently cannot easily tell whether the active Private session has:

- Foundational Field;
- VexLife Memory CURRENT;
- VexLife Memory HELD;
- no provider Memory content;
- only private-session history.

A small Product projection may be earned if it helps answer the human question without making the renderer a second owner.

Possible presentation shape:

~~~text
Private
  Conversation history: not included
  VexLife Memory: Current / Held
  Foundational ground: included
~~~

The source owners remain unchanged.

### 4. "Private · Clean slate" is a separate possible control

Victor identified a potentially cleaner experiment than ordinary-thread context horizon / restore:

~~~text
Private
  current:
    ephemeral private transcript
    + Foundational Field
    + reply language
    + current VexLife Memory when available

Private · Clean slate
  proposed:
    ephemeral private transcript
    + Foundational Field
    + reply language
    + NO VexLife Memory provider content
    + NO ordinary canonical Conversation
~~~

This would mean:

~~~text
DO NOT SUPPLY PERSONAL CONTINUITY
!= DELETE MEMORY
!= WIPE MODEL WEIGHTS
!= REMOVE FOUNDATIONAL FIELD
~~~

No implementation is authorized by this note.

Before adding a Control, answer:

- Is "Clean slate" a stable human intention or only a test affordance?
- Does it belong as a mode of Private continuity input, or as a separate semantic owner?
- Does disabling Memory for one private session need a receipt/projection so the person can verify it?
- What exact provider evidence proves Memory was not supplied?
- Does leaving Private retain the existing private-gap explanation without creating a new continuity contradiction?

### 5. Memory formation remains separate debt

The foundation index currently records that Product-side Memory formation is absent while the existing reader can project current/held truth.

Do not "fix Private continuity" by silently promoting arbitrary Conversation into Memory.

~~~text
CONVERSATION
!= MEMORY

PRIVATE UX DEBT
!= MEMORY FORMATION AUTHORITY
~~~

## Relationship to current work

- #162 — continuity/context hypothesis and lived Vex feedback. This note is a concrete debt discovered from that broader work.
- #177 — thread-context horizon refinement. Its source/build proof is separate; its Restore/re-entry semantics remain held for review.
- #154 — preserved predecessor of #177.
- #171 — MLX runtime/cache pressure. Cache reuse should remain semantically neutral to the logical prompt and does not own Private continuity meaning.
- #153 / #173 — model/chat-template and eventual cross-model qualification; useful later for comparing continuity interpretation, not owners of Private semantics.

## Proof before any Product change

A future executable refinement should prove at least:

~~~text
Private + Memory CURRENT
-> provider receives exact current Memory packet
-> UI says Memory is available, without claiming subjective recall

Private + Memory HELD
-> provider receives no Memory content
-> UI does not imply that prior personal continuity is available

Private session
-> canonical ordinary Conversation is absent from provider prompt

Private exit
-> transcript is not added to canonical Conversation
-> bounded private interval / agreed notes behave exactly as current contract says

Private · Clean slate, if earned
-> no VexLife Memory provider content
-> Memory durable source unchanged
-> Foundational Field relationship unchanged
-> exact receipt/projection proves the omission
~~~

A screenshot alone does not prove provider context. Use prompt/context evidence for that claim.

## Current disposition

DRAFT TECH DEBT / NO PRODUCT BEHAVIOR CHANGE

The immediate correction is understanding:

~~~text
"Vex still remembers what it already knew"
is not a safe Product invariant today.
~~~

The next source change, if any, should start from the exact continuity source owners above rather than from the current wording.

<!-- [VXG RealForever][VEXLIFE-PROD][PRIVATE][MEMORY][TECH-DEBT] -->
