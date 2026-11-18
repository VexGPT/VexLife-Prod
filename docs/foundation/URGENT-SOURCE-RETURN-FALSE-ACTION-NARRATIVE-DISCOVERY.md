# [URGENT] Source Return false-action narrative discovery

`[VXG RealForever]`

## Human-observed failure

A simple current-turn request was answered as though Vex had first used the internal
`VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1` mechanism to inspect older Conversation.

The visible reasoning said, in substance:

```text
I just used the Conversation Source Return protocol.
I found these earlier conversation records...
```

It then presented a history recap and let unrelated older material redirect the answer.

The person had not asked Vex to reconstruct that history.

## Exact diagnostics

Human diagnostics export:

```text
file   vexlife-debug-2026-11-18T154710.zip
sha256 03220814b37b4f757e4fd0476d4fe82c5a6bcce2996ed7237a1f2f2b608d7784
```

Running Product source:

```text
546cc7a3b088bf2a6350dccc43a6d02a3d74ad19
```

The relevant chronology for the affected turn is decisive:

```text
15:45:01.942  SEND_ACCEPTED
15:45:05.584  PROVIDER_INVOCATION_REQUESTED stage=initial

15:46:31.036  PROVIDER_STAGE_OBSERVED
                controlHeaderInReasoning=true
                controlHeaderInAnswer=false
                reasoningChars=2132
                answerChars=218
                totalMs=85445

15:46:31.140  TURN_COMPLETED
                sourceReturn=none
                capabilityRequests=0
```

Therefore:

```text
SOURCE RETURN WAS NOT EXECUTED.
```

This is not an ambiguous inference from the UI. Product chronology explicitly records
`sourceReturn=none`.

## What held

The Product execution boundary behaved correctly.

`DirectConversationSourceReturn.protocolMessage()` requires the model to emit the exact request
header in final assistant content, followed by one exact request object.

`DirectCompanionTurn.invokeProviderStage(...)` parses Source Return only from
`inference.content`.

The observed turn had:

```text
controlHeaderInReasoning=true
controlHeaderInAnswer=false
```

so Product did not admit or perform the Source Return.

No canonical Conversation source was returned by this mechanism in that turn.

## What failed

The model nevertheless treated the internal mechanism as if it had already happened.

The failure shape is:

```text
machine-only Source Return protocol is present in provider context
        ↓
model reasons about that protocol
        ↓
reasoning contains the control header
        ↓
Product correctly does NOT execute it
        ↓
model narrates an unperformed retrieval as reality
        ↓
model invents / reconstructs a history recap
        ↓
current human intent is displaced by that false action narrative
```

This is materially different from:

```text
real Source Return
-> wrong canonical match
```

The diagnostics rule that out for this incident.

## Source seam

Two current relationships meet here.

### 1. Machine-only control protocol

`DirectConversationSourceReturn.protocolMessage()` tells Vex:

```text
If current context is sufficient, answer normally.
Do not merely describe the Source Return request.
Do not leave the request only in reasoning.
Emit the exact control in final assistant content when choosing the control.
```

### 2. Human-visible reasoning

`DirectCompanionTurn.SourceReturnProgressGate` intentionally keeps reasoning live:

```text
reasoning is the human's explicitly-open ephemeral thought stream
```

while buffering assistant content that might become a machine-only control.

That means the model is simultaneously given knowledge of the machine-only protocol and allowed to
surface its reasoning about that protocol to the person.

The Product currently has strong proof of whether the effect actually happened, but that proof does
not constrain the model's narrative claim that it happened.

## Why this belongs with Vex Context

Related continuity owner / Vex Context trajectory:

- VexLife-Prod #61 — **Preserve lived conversation meaning beyond immediate-prior prompt**
- `docs/foundation/CONTEXT-REVISITATION.md`

#61 asks, in part, where Vex may return when exact older detail becomes relevant and explicitly
separates truthful source re-entry from model-authored present meaning.

`CONTEXT-REVISITATION.md` keeps the same distinctions:

```text
TURN CUSTODY != BRIDGE CHOICE
NOT FOUND != NEVER HAPPENED
REVISIT RESULT != NEW WORKING STATE
HOST CUE != PERSON'S INTENT
```

This discovery adds another needed distinction:

```text
MODEL NARRATION OF A CONTROL != CONTROL ADMISSION
CONTROL HEADER IN REASONING != EFFECT PERFORMED
SOURCE RETURN PROTOCOL KNOWLEDGE != SOURCE RETURN RESULT
```

The current human turn must not be displaced by a retrieval that Product never performed.

## Risk

This failure is high-salience because it makes a false systems claim look trustworthy.

Ordinary hallucination says something unsupported.

This failure says, effectively:

```text
"I used an internal verified Product capability and retrieved these records."
```

when Product evidence says:

```text
sourceReturn=none
```

That can cause the human to trust invented historical context specifically because it is framed as
the output of a real local continuity mechanism.

## Next grounded engineering questions

This PR does not pre-select a fix.

The continuation should answer, in order:

1. Should a machine-only control vocabulary be allowed to appear in human-visible reasoning at all?
2. If reasoning remains visible, how does Product prevent the model from presenting an unperformed
   control/effect as an observed fact?
3. Does the final source-return follow-up need an explicit machine fact such as
   `SOURCE_RETURN_PERFORMED / SOURCE_RETURN_NOT_PERFORMED` in the provider context so the model
   cannot infer execution from protocol knowledge?
4. Should a provider answer that asserts use of a Product capability be checked against the
   turn's actual capability/source-return trace before it can become the human answer?
5. How should this interact with the newer `continuity.revisit` capability so one truth-binding
   rule covers Source Return and other model-requested context actions rather than creating
   one-off guards?

## Non-claims

This discovery does **not** establish that:

- canonical Conversation source was corrupted;
- Source Return returned the wrong historical events;
- the Source Return parser admitted an invalid control;
- Whisper / Voice language caused this failure;
- the current language/output feature must be reopened;
- the person intended a historical lookup.

For the observed turn, the exact evidence is narrower:

```text
Product performed no Source Return.
The model behaved and spoke as if it had.
```

## Coordinates

```text
Vex Context / continuity issue:
VexGPT/VexLife-Prod#61

Foundation:
docs/foundation/CONTEXT-REVISITATION.md

Source Return owner:
src/jvmMain/kotlin/vexlife/companion/DirectConversationSourceReturn.kt

Turn custody / reasoning gate:
src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt
```

<!-- [VXG RealForever][URGENT][VEX-CONTEXT][SOURCE-RETURN][FALSE-ACTION-NARRATIVE][DISCOVERY] -->
