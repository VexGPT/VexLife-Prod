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
  ├─ current Conversation snapshot ──────┐
  ├─ current accepted Memory (read-only) │
  ├─ current Context Profile             │
  └─ current qualified native runtime    │
                                         ↓
                              disposable shadow Home
                                + scratch Conversation
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

Labs injects only a scratch policy into that shared component. Ordinary UI sends keep the
original first-slice reach boundary; an explicit experiment Send may widen only the read-only
relationships named below:

~~~text
ordinary Lab Send
  source return              held
  external capability reach  held
  reasoning continuation     Product default ON

explicit reach experiment Send
  source return              caller-selected for this scratch turn
  generic capability reach   caller-selected for this scratch turn
  reasoning continuation     caller-selected ON / OFF for A/B evidence

Thinking                   Lab-selected when provider-qualified
reasoning depth            Lab-selected from exact provider choices
temporary Memory cap       Lab-selected
Context Profile ceiling    Lab-selected
capability working block   inherited from captured Context Profile
Home                       scratch shadow
runtime                    same qualified native runtime
~~~

The experiment flags are not durable settings and are false/true by defaults that preserve ordinary
Labs behavior. Enabling them does not create a second turn engine: the same DirectCompanionTurn,
DirectPromptContext, provider representation, Capability Bridge and Conversation Source Return owners
execute the scratch turn.

The Product asserts that the real Conversation head has not changed after each Lab turn. When current
Memory is used, it also asserts that the observed real Memory fingerprint did not change.

Scratch data is deleted when the Lab is cleared/reloaded or the service closes.

## Current Lab choices

The first slice intentionally exposes every known field while granting editability narrowly.

The quick configuration is ordered by ordinary human likelihood rather than internal source order:

~~~text
Thinking
Reasoning depth
Active context
Structured Memory
Memory prompt depth
~~~

The deeper field catalog follows the same bias: likely human adjustments first, then restart-bound
controls, held knobs, and finally read-only architecture/runtime facts. This is presentation order,
not authority order; each field still names its rightful owner.

### Thinking

When the selected model/provider exposes a qualified request-local thinking switch, Labs can turn it
on or off for the next scratch inference.

~~~text
LAB THINKING SWITCH
!= DURABLE PRODUCT SETTING
!= VIEWABLE THOUGHTS PREFERENCE
~~~

The selected model owns whether that switch exists. Current Product relationships are explicit:

~~~text
Qwen 27B  -> On / Off
Qwen 9B   -> On / Off
Qwen 4B   -> On / Off
GPT-OSS   -> no Qwen-style On / Off switch claimed
~~~

Turning Thinking off sends no reasoning-depth value. It does not delete any previously retained
Conversation or reasoning evidence.

### Reasoning depth

Labs shows only the exact provider-template tiers the selected model has earned:

~~~text
Qwen 27B  -> Low / Medium / XHigh
Qwen 9B   -> Fixed by model; no qualified tier selector in the pinned template
Qwen 4B   -> Fixed by model; no qualified tier selector in the pinned template
GPT-OSS   -> Low / Medium / High
~~~

XHigh is kept as the provider's own vocabulary and may be presented to the human as the deepest
tier. Labs never invents a Medium/High/XHigh ladder for a model whose exact pinned template does not
expose one.

Thinking and reasoning depth are request-local experiment controls in this slice. They are not added
to Context Profile persistence merely because Labs can test them. A durable Product Control is a
separate earned effect.

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

After testing, **Save supported settings** may persist the already-earned active-context setting
through the existing Context Profile owner. Product Default is saved as a new named profile rather
than rewritten. Thinking, reasoning depth, and Memory prompt depth remain Lab-only until their own
durable Product Controls are qualified. The button is enabled only when the current draft exactly
matches the configuration that produced the latest completed Lab reply; changing any test value
requires a new Lab turn before saving.

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
CURRENT_SNAPSHOT
  exact canonical Conversation/context copied into the disposable shadow
  real currentness is checked before/after the copy

EMPTY
  clear the scratch Lab Conversation
  real archives remain unchanged
~~~

**Reload current conversation** discards the current scratch lineage and forms a new exact snapshot
from the canonical Conversation.

**Restore defaults** restores the Lab's Product/context defaults, current Memory mode, and a fresh
current-Conversation snapshot. It does not erase saved profiles or Memory.

## Capability boundary

Ordinary Labs remains conservative: browser context, Conversation Source Return and generic
capability requests are held unless one Send explicitly opts into the experiment boundary.

The capability-enabled experiment is still read-only and scratch-owned:

~~~text
scratch human request
-> real DirectCompanionTurn
-> current qualified provider representation

generic capability request, when explicitly admitted
  web / repository       -> read-only outside source
  Home                   -> scratch Home only
  Memory                 -> selected Lab Memory mode
                             CURRENT = read canonical Memory without mutation
                             NONE    = scratch/empty Memory relationship
  continuity.revisit     -> this scratch turn's Conversation / Memory / Working Continuity

Conversation Source Return, when explicitly admitted
  -> searches/reopens the scratch Conversation snapshot
  -> never appends to canonical Conversation

result
-> later provider stage of the same scratch human turn
-> same runtime binding
-> same Foundation / Context Profile capture
-> same reasoning-continuation policy for this A/B arm
~~~

Every completed experiment returns ephemeral evidence for all provider stages, the read-only
capability uses, and the Source Return state/source refs. The Lab still re-observes the canonical
Conversation head and, when current Memory participates, its semantic fingerprint to prove neither
moved.

~~~text
LAB REACH
!= PRODUCT EFFECT AUTHORITY

READ CANONICAL MEMORY
!= MUTATE CANONICAL MEMORY

SOURCE RETURN IN LAB
= SCRATCH CONVERSATION RETURN
!= CANONICAL CONVERSATION MUTATION

REASONING CONTINUATION OFF
= EXPERIMENTAL PROVIDER-CARRIAGE COMPARISON
!= DELETE CONVERSATION / MEMORY / DURABLE PRODUCT POLICY
~~~

This boundary exists specifically so lived testing can observe the whole
perceive -> reach -> return -> interpret -> reach-again -> answer circuit instead of judging model
intelligence from an answer-only or single-inference harness.

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
