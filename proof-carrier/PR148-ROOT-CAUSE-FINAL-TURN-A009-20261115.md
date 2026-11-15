[VXG RealForever]

# PR148 Root Cause — Final Output / Retry / Thought-History Failure

## Scope

This document investigates the lived state where:

```text
human Send
-> live reasoning works
-> capability requests/results work
-> Vex continues reasoning
-> final ordinary answer never appears
-> attempt eventually fails/reconciles
-> browser returns the original prompt into the composer
-> live thought/process history disappears
```

Current Product candidate under investigation:

- repository: `VexGPT/VexLife-Prod`
- PR: `#148`
- head: `c2e9aa6eb830152a37f6b1f4897148657d919e3a`
- tree: `da7036b00f27bc3043d28159319e1bfffd45c905`
- base: `035335a34d4220d6d3242c088a1a9667c29f135f`

A009 canonical RETURN:

- SHA-256: `dc6d866264a9da2f78d8400355ea2b8ba0c65369b93d42b9ade926bd2fc3ac9f`
- terminal observation: `SAFE_FAILURE / WAITING_FOR_FIRST_TURN`
- exact source preflight: PASS
- native Setup -> exact Product currentness: PASS
- Product source: `SOURCE_BOUND_LISTENING`
- active model: certified 27B

Victor's screenshots from that same run show materially more lived progress than the A009 terminal summary:
- live reasoning streamed;
- GitHub capability request projected;
- invalid capability request projected truthfully;
- Vex corrected itself;
- repository describe succeeded;
- README read succeeded;
- Vex continued reasoning after returned capability truth;
- ordinary final response never appeared.

## VexArchitecture expected relationship

The relevant relationship is:

```text
Human Send
-> Conversation owner admits exact request identity
-> provider stage
-> capability request
-> capability owner returns truth
-> same Conversation turn continues
-> final provider result
-> canonical response/context commit
-> TURN_COMPLETED or TURN_FAILED
-> LocalService projects terminal truth
-> browser presents canonical outcome
-> human may continue / retry through the exact next lawful route
```

Presentation is an observer. It may preserve session-only process/reasoning, but it must not destroy or reinterpret canonical outcome semantics.

## Root cause 1 — PROVEN: presentation destroys live attempt state before canonical reconciliation is known

Current browser source:

`src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt`
blob at current head:
`3bb9ffa0a183f6b76f57e26361c5669ba803075f`

In `send()`, any streaming exception enters the catch block and immediately does:

```text
state.streamPhase = null
state.streamedContent = ''
state.streamedReasoning = ''
state.liveReaches = []
state.liveProcessEntries = []
state.liveProcessExpanded = true
state.liveProcessManuallyToggled = false
resetLiveProcessViewports()
```

Only **after that destruction** does the browser ask whether the exact Send can reconcile through `reconcileSendOutcome(pending)`.

This ordering is wrong.

It means an ambiguous transport ending can erase the human's complete session-scoped process history even if the canonical owner later reports that the Send actually completed.

Worse: when reconciliation succeeds, the browser calls:

`completeCanonicalSend(reconciled, false)`

Because `streamed=false` and `liveProcessEntries` were already erased, the recovered completed response cannot inherit the process history the human just watched.

Therefore:

```text
presentation transport ambiguity
-> browser destroys ephemeral evidence
-> canonical truth checked afterward
```

This reverses the VexArchitecture relationship. Observation/presentation is mutating its own evidence before rightful owner truth has returned.

## Root cause 2 — PROVEN: browser collapses canonical retry semantics into "put prompt back in draft"

Canonical turn owner:

`src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt`
blob:
`90da841311e6b22c252d6b104d898fd0519cf5c5`

On a substantive failed Send, the owner writes a failure receipt with:

```text
requestDurablyRecorded = true
currentHeadCommitted = false
retrySameSendAttemptAllowed = false
exactNextSafeRoute =
  FORM_NEW_SEND_ATTEMPT_AND_RETRY
  or
  RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT
```

Tests explicitly prove this intent:
- same failed idempotency key must not call provider again;
- the failed request remains in abandoned-attempt state;
- a **new Send identity** may retry the request;
- if a prior canonical head exists, a new attempt resumes from that last valid head.

This is not "the user cannot retry."

It means:

```text
same attempt identity cannot replay
!=
human request cannot be tried again
```

The LocalService preserves the owner's exact route in its 409 response:
- `retrySameSendAttemptAllowed`
- `exactNextSafeRoute`

Current browser ignores `exactNextSafeRoute`.

It reduces terminal meaning to:

```javascript
retireAttempt = status === 400 || retryAllowed === false
```

Then:

```text
retireAttempt
-> state.pending = null
-> if draft empty, ui.draft.value = pending.content
-> status "Send failed"
```

So the browser turns:

```text
FORM_NEW_SEND_ATTEMPT_AND_RETRY
```

into:

```text
erase the failed attempt presentation
put old words back in the compose box
make the human manually start over
```

That is the observed "revert prompt" behavior.

The semantic owner has a more precise recovery route than the presentation exposes.

## Root cause 3 — PROVEN provenance: failure cleanup predates the richer process UX and was never upgraded with it

Merged main before PR #136 already cleared streamed reasoning/content on failure and returned the prompt to the draft.

PR #136 introduced exact-Send reconciliation, but still cleared stream presentation **before reconciliation**.

PR #141 added structured provider/process-stage presentation.

PR #143 replaced stage-centric projection with human-readable process entries.

PR #146 fixed live DOM identity/scroll ownership during streaming.

But the failure path retained the old cleanup semantics and simply expanded the reset to include the new process structures.

Thus the architecture evolved asymmetrically:

```text
success path:
  preserve/freeze process
  bind it to completed response
  allow same-session View Process

failure/reconcile path:
  erase process before owner truth
  erase reaches/reasoning
  return prompt to composer
```

The old failure-state policy became increasingly destructive as richer process state was added.

## Root cause 4 — STRONGLY SUPPORTED trigger, NOT YET CERTIFIED from A009 chronology:
GitHub PR intent exceeds the current GitHub capability's actual scope and burns bounded request stages

Current capability owner:

`src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt`
blob:
`9199b29957b492105a5c052ea2c41f37ea03ccfb`

Current GitHub capability definition:

`src/jvmMain/kotlin/vexlife/capability/VexLifeReachDefinitions.kt`
blob:
`1e85ca72da4f8f9e7e7f03c0ad6dc65f90c3486f`

GitHub actions are only:

```text
describe
tree
read
```

Purpose:
read public repository description/tree/file text.

There is no PR/issue object action.

The lived user request asked Vex to inspect GitHub/PR #141.

Screenshots show:
1. Vex first emitted an invalid action resembling `repo_describe`;
2. the bridge truthfully returned INVALID_REQUEST;
3. Vex corrected to repository `describe`;
4. Vex read README.md;
5. Vex then reasoned about trying to inspect PR #141 through another path.

Important capability-budget semantics:
- `MaximumRequestsPerTurn = 4`;
- malformed/invalid requests still consume a request number;
- the prompt tells Vex "Up to 4 requests per turn";
- the loop permits one extra request to receive a typed `LIMIT_REACHED` result;
- another capability request after exhaustion throws `CapabilityRequestLimitException` and can fail the human turn.

Therefore a goal whose natural object is a Pull Request can cause Vex to spend long provider stages exploring repo-file actions that cannot directly satisfy the goal.

This is a strong explanation for:
- many minutes of reasoning/reaching;
- no early final content;
- bounded turn not terminalizing before A009's 650-second observation window;
- possible later terminal failure after request exhaustion.

However, A009 did **not** retain the exact Home chronology records. It retained only the count (20). Therefore it is not yet source-proven whether this exact lived turn:
- reached LIMIT_REACHED;
- threw CapabilityRequestLimitException;
- hit a later connection failure;
- or was simply still inside another provider stage when A009 ended.

Do not upgrade this trigger from "strongly supported" to "certified" without the exact chronology/failure receipt.

## Root cause 5 — PROVEN test gap: web tests protect failure booleans, not failed-attempt continuity

`src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt`

Current tests assert source presence of:
- `retrySameSendAttemptAllowed`;
- `retireAttempt = status === 400 || retryAllowed === false`;
- reconciliation.

They do not prove:
- failure process history remains visible;
- live process is snapshotted before reconciliation;
- `exactNextSafeRoute` is presented;
- a new-idempotency Try Again control is offered;
- failed attempt thought/reach state remains session-scoped;
- successful reconciliation after stream loss inherits the streamed process history.

The canonical-turn tests already prove the correct new-attempt recovery semantics. The presentation contract lacks matching tests.

## Correct relationship / implementation target

Do not change raw reasoning persistence policy. Raw reasoning remains browser-session only.

### Presentation state should be monotonic until canonical outcome is known

Instead of:

```text
stream error
-> erase live process
-> reconcile
```

use:

```text
stream error
-> freeze/snapshot current live process session state
-> keep pending attempt identity
-> ask canonical owner
```

Then branch on owner truth.

### If reconciliation returns COMPLETED

```text
canonical completed response
-> bind frozen process snapshot to responseMessageRef
-> preserve same-session reasoning/process history
-> refresh Conversation/Context
-> clear transient live slot only after transfer
-> render final Vex bubble
```

Even though recovered reasoning is not durably available, the browser already possesses the live process from this session and may keep it session-scoped.

### If canonical owner returns terminal FAILED

Do not convert it to anonymous draft text.

Form a session-only failed-attempt presentation:

```text
human request
+ failure code
+ exactNextSafeRoute
+ frozen process/reaches/reasoning
+ failed attempt identity
```

Render the failed attempt in the timeline.

Offer:
`Try Again`

That control must form a **new idempotency key** while carrying the same human content and following `exactNextSafeRoute`.

Do not replay the failed key.

### If outcome is still unresolved

Keep:
- same `pending` idempotency key;
- frozen/live process history;
- "Reconciling Send" / unresolved status.

Do not put the prompt back into a fresh draft because canonical terminal truth is not known yet.

### Only clear process state after ownership transfer

The browser may reset `liveProcessEntries`, reaches and viewport state only after one of:
- transferred to completed response session state;
- transferred to failed-attempt session state;
- explicit human dismissal/new-session loss.

## Capability-side follow-up, only after exact chronology confirms it

If chronology shows capability exhaustion is the canonical trigger, solve the rightful capability mismatch rather than merely increasing limits.

Potential routes to evaluate:
1. teach the frame explicitly that `github` reads repository files only and does not read PR/issue objects;
2. tell Vex to use `web.fetch` on a public GitHub PR URL when it needs PR page text;
3. add a rightful GitHub PR read action only if there is a real owner/consumer need;
4. preserve invalid-request truth, but reconsider whether model syntax-correction attempts should consume the same scarce semantic budget as successful reads.

Do not implement all of these by default. First read the exact failed-turn chronology.

## Exact next evidence

From the Home used in A009, inspect:
- chronology records for the exact send;
- `recovery/direct-companion/<turnRef>.json` if present.

Decision:

```text
TURN_COMPLETED exists
-> canonical turn is good;
   root cause is terminal SSE/browser completion projection.

TURN_FAILED exists
-> use exact failureCode and exactNextSafeRoute;
   preserve failed process + offer new-id Try Again.

Neither exists
-> turn was still executing/stuck;
   inspect last PROVIDER_INVOCATION_REQUESTED and capability exchange number.
```

## Preservation boundary

Do not regress:
- repaired startup/runtime;
- live reasoning;
- capability request/result visibility;
- stable live process DOM;
- scroll ownership;
- same-turn runtime identity;
- session-only raw reasoning policy;
- browser observer isolation from provider execution.

The repair target is **failure/reconciliation ownership**, not startup and not the live process renderer.
