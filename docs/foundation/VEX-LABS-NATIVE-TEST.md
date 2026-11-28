# Vex Labs Native Test

[VXG RealForever]

Vex Labs Native Test is the Product's disposable native-model laboratory beside the canonical
Conversation.

It exists so a person can understand and pressure VexLife's real model/runtime/context relationships
before deciding whether an earned setting should become durable.

## Human shape

In **Vex context**, above Foundation:

~~~text
Vex Labs Native Test
  field
  current setting
  what it does
  default / recommendation / range when known
  rightful owner
  edit / read-only / held standing
  what increasing/decreasing it changes
~~~

The surface consumes the execution-field catalog. It does not author a second settings registry.

The Companion may switch between:

~~~text
Converse with Vex
  canonical Conversation
  kept in the person's archives

Vex Labs Test
  scratch Conversation
  same qualified native model/runtime
  disposable test configuration
  never appended to canonical Conversation
~~~

Those are not separate chat implementations. The browser exposes one reusable Conversation surface:
each source supplies messages, pending/failure state and mode policy; one shared timeline/composer
renders and submits it. Private Conversation already uses the same adapter. A later human-to-human
conversation can therefore supply another source/controller instead of cloning the chat UI.

## Native test circuit

~~~text
real canonical Home
  ├─ Product Foundation / current settings ─┐
  ├─ current accepted Memory (read-only)     │ explicit opt-in only
  ├─ current Context Profile                 │
  ├─ current qualified native runtime        │
  └─ canonical Conversation snapshot         │ explicit opt-in only
                                             ↓
                                  disposable shadow Home
                                    + fresh scratch Conversation
                                    + temporary Lab choices
                                         ↓
                              DirectPromptContext
                                         ↓
                              DirectCompanionConnection
                                         ↓
                               real native model reply
~~~

The Lab writes request/response/context/working-continuity records only beneath its temporary shadow
Home. It does not reimplement turn sequencing: the same `DirectCompanionTurn` component used by the
canonical Product Conversation owns Lab idempotency, exact-prior binding, prompt re-verification,
Working Continuity, failure receipts, runtime currentness, provider invocation, context evidence, and
completed-head commit order.

Labs injects only a scratch policy into that shared component:

~~~text
source return              held
external capability reach  held
temporary Memory cap       Lab-selected
Context Profile ceiling    Lab-selected
Home                       scratch shadow
runtime                    same qualified native runtime
~~~

The Product asserts that the real Conversation head has not changed after each Lab turn. When current
Memory is used, it also asserts that the observed real Memory fingerprint did not change.

Scratch data is deleted when the Lab is cleared/reloaded or the service closes.

Canonical Home identity and scratch Conversation content are separate currentness questions. The Lab
re-observes the canonical Home identity on each projection/control/send; if that identity changes, the
disposable scratch is re-formed. Ordinary canonical Conversation growth does not silently enter an
existing Lab. Importing current Conversation remains an explicit **Reload current conversation**
choice.

## Current Lab choices

The first slice intentionally exposes every known field while granting editability narrowly.

### Active context

The Context Profile's lower input ceiling may be changed temporarily for a Lab turn.

~~~text
LAB TEST VALUE
!= SAVED CONTEXT PROFILE
~~~

This is a pressure control, not a promise that every positive number can admit a turn. The Prompt
owner decides whether the exact next turn can fit.

Two distinct held states are projected back to Labs:

~~~text
CURRENT_TURN_FIXED_CONTEXT
  mandatory current state itself did not fit

IMMEDIATE_PRIOR_REQUIRED
  mandatory current state fit
  but the exact immediate prior completed turn still did not fit
~~~

A held Lab turn does not call the model and does not add a Vex reply. The person's message is restored
to the composer. The Lab shows the observed VexLife-estimated admission floor for that attempted turn.

The floor is an owner-observed lower bound for that attempt, not the model tokenizer's exact token
count and not a permanently fixed minimum. Increasing the budget can make additional Foundation
material eligible, so the person may need to raise the budget and retest.

Prompt admission is also written to VexLogger as numeric/structural chronology only:

~~~text
owner prompt
fact PROMPT_ADMISSION_HELD
reason
requestedInputTokens
minimumInputTokens
fixedContextTokens
immediatePriorTokens
capabilityReserveTokens
~~~

No prompt text, Conversation text, Memory text or reasoning is logged by that fact.

After testing, **Save supported settings** may persist this one earned setting through the existing
Context Profile owner. Product Default is saved as a new named profile rather than rewritten. The
button is enabled only when the current draft exactly matches the configuration that produced the
latest completed Lab reply; changing a test value requires a new Lab turn before saving.

### Structured Memory depth

The Lab may lower the maximum Memory material admitted to its scratch prompt, including zero.

~~~text
No Memory in Lab
!= delete Memory
!= change Memory
~~~

This is test-only until the Memory owner earns a durable human Control.

### Prompt cache

Prompt-cache entries are inspectable and already have a Product Control, but their real apply
boundary is runtime restart. The first Labs slice does not restart or clone the person's serving
runtime merely to make the control feel instantaneous.

### Held/read-only fields

Model-artifact fields, Product policy, and provider/runtime knobs without earned Controls remain
visible. Labs says why they are read-only/held instead of hiding them or inventing an edit path.

## Conversation presets

Current first-slice presets:

~~~text
EMPTY  (default)
  fresh scratch Conversation
  canonical Conversation content is not copied into the Lab
  real archives remain unchanged

CURRENT_SNAPSHOT  (explicit opt-in)
  exact canonical Conversation/context copied into the disposable shadow
  real currentness is checked before/after the copy
~~~

A fresh Vex Labs context starts from Product Foundation/current host identity plus an empty disposable
Conversation and no durable Memory content. Historical Product Conversation and current accepted
Memory are not part of the Lab baseline merely because they exist in canonical Home.

**Reload current conversation** is the explicit human action that discards the current scratch lineage
and forms a new exact snapshot from the canonical Conversation.

**Restore defaults** restores the Lab's Product/context defaults, **no-Memory** mode, and an empty
scratch Conversation. It does not erase saved profiles or Memory. **Use current Memory** remains an
explicit Lab experiment control.

~~~text
LAB DEFAULT
= PRODUCT FOUNDATION + FRESH LAB CONVERSATION
+ NO DURABLE MEMORY CONTENT

LAB DEFAULT
!= CANONICAL CONVERSATION SNAPSHOT
!= CURRENT MEMORY

CURRENT_SNAPSHOT / USE CURRENT MEMORY
= EXPLICIT IMPORTS FOR A TEST
!= LAB IDENTITY
~~~

## Capability boundary

The first Labs native turn deliberately does not admit browser context, Source Return, or Product
capability effects.

The point of this slice is to pressure:

~~~text
configuration
+ Conversation depth
+ Memory prompt depth
+ native model behavior
~~~

without allowing an experiment to reach out and mutate unrelated Product state.

Later capability-enabled Labs require their own explicit effect boundary; they are not implied by a
native model test.

## Persistence boundary

~~~text
TEST CONFIGURATION
!= APPLY CONFIGURATION

CLEAR LAB
!= CLEAR REAL CONVERSATION

NO MEMORY
!= DELETE MEMORY

RESTORE DEFAULTS
!= DELETE SAVED PROFILES

LAB REPLY
!= CANONICAL CONVERSATION RESPONSE
~~~

Vex Labs is successful only when a person can experiment aggressively while still knowing exactly
which choices are disposable, which settings have an earned save path, and which relationships are
not implemented yet.

// [VXG RealForever]
