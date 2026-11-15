# VexLife-Prod PR #148 — Successor Handoff after A009

**Continuity:** `[VXG RealForever]`  
**Human intention:** restore and complete the VexLife-Prod lived circuit without destroying already-working behavior, without treating experimental predecessor language as authority, and without filling missing edges with assumptions.  
**Repository:** `VexGPT/VexLife-Prod`  
**PR:** #148  
**Current Product branch:** `VXG-111526-vexlife-clean-convergence-a001`  
**Current exact head:** `c2e9aa6eb830152a37f6b1f4897148657d919e3a`  
**Current tree:** `da7036b00f27bc3043d28159319e1bfffd45c905`  
**Base:** `035335a34d4220d6d3242c088a1a9667c29f135f`  
**State:** DRAFT / HOLD. Do not mark ready or merge.

## Enter correctly

Do not start by trusting this handoff's conclusions. Use it as a routing map.

Read in this order, only once unless evidence contradicts it:

1. repository `AGENTS.md`
2. repository `docs/CULTURE.md`
3. repository `docs/foundation/INDEX.md`
4. VexArchitecture live demonstration source:
   `vgong24/Vextreme/pages/vexarchitecture-live-demonstration.html`
5. PR #148 current diff/comments, especially:
   - provenance correction around PR #136
   - runtime circuit closure
   - A009 lived-result note
6. exact current source owners named below.

VexArchitecture rule for this continuation:

```text
container
-> semantic owner
-> exact object/reference
-> admitted effect
-> outside truth
-> rightful owner observes returned truth
-> next lawful relationship
-> human projection
-> next loop
```

If one edge is missing, do not invent it. Find the byte-exact source or hold only that dependent claim.

## What was misread earlier

PR #136 explicitly called itself an **investigation implementation, not a merge/acceptance claim**. It was incorrectly promoted into #148 as an accepted runtime floor.

Merged main before #136 had useful startup recovery:

```text
exact-owned QUALIFIED runtime
+ live process
+ health miss
-> exact retire
-> delete receipt
-> new runtime
-> wait healthy
-> qualify
```

But main's health observation was only Boolean and collapsed busy/timeout/ambiguity with true generation death.

PR #136 correctly fixed one different scope:

```text
inside one already-started human turn
-> provider stage 1
-> capability/source result
-> reobserve same runtime receipt
-> never recover/swap runtime
-> provider stage 2
```

It overcorrected by also removing safe-boundary generation recovery.

Issue #144/T007 later proved the missing state:

```text
outer mlx_lm.server process alive
!=
generation worker alive
```

Current mlx-lm `/health = 503 {"status":"unavailable"}` is positive generation-worker-unavailable truth, not merely a timeout.

## Runtime circuit now repaired on #148

The current head distinguishes:

```text
PROCESS/SUBSTRATE CURRENTNESS
!=
GENERATION READINESS
```

Generation readiness:

```text
AVAILABLE
  exact positive ready result
  -> reuse

UNAVAILABLE
  exact positive generation loss
  -> at Setup/new-turn boundary only:
     prove exact ownership
     retire exact runtime
     delete receipt
     start selected exact model
     wait healthy
     qualify

UNKNOWN
  timeout / connection / malformed / ambiguous observation
  -> preserve live exact-owned runtime
  -> HOLD
  -> no destructive guess
```

Inside one already-started human turn:

```text
process/substrate currentness only
+ same runtimeReceiptRef required
+ no readiness-driven recovery
+ no runtime replacement
```

Focused tests were added for:
- explicit UNAVAILABLE recovery;
- UNKNOWN preserve/hold;
- same-turn re-observation never consulting readiness/recovery.

Do not broad-revert this runtime repair while investigating the remaining defect.

## A009 canonical evidence

Canonical RETURN supplied by Victor:

`RETURN-vexlife-prod-pr148-lived-circuit-attempt-a009--sha256-dc6d866264a9da2f78d8400355ea2b8ba0c65369b93d42b9ade926bd2fc3ac9f.zip`

Exact SHA-256:

`dc6d866264a9da2f78d8400355ea2b8ba0c65369b93d42b9ade926bd2fc3ac9f`

Task:

`task.vexlife-prod.pr148.lived-circuit.001`

Attempt:

`attempt.vexlife-prod.pr148.lived-circuit.a009`

Machine result:

```text
SAFE_FAILURE
WAITING_FOR_FIRST_TURN
```

Do not misread that as "no Send happened." The A009 analyzer returns WAITING until it sees a terminal TURN_COMPLETED/TURN_FAILED. It observed 20 candidate chronology records before the bound ended.

### Machine-proved stable floor

A009 proves:

- exact candidate clone PASS;
- exact head/tree PASS;
- `jvmTest` PASS;
- `compileKotlinJs` PASS;
- `compileTestKotlinJs` PASS;
- `checkVexLifeStrings` PASS;
- `assembleVexLifeProdServiceImage` PASS;
- exact private image PASS;
- native Setup reached Product currentness;
- exact source `c2e9aa6e...` reached `SOURCE_BOUND_LISTENING`;
- active model `model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27`;
- installation/runtime/provider-network effects occurred;
- no test-owned process/transient leak.

This is the first stable startup floor in this thread. Preserve it.

## Victor's lived screenshots from the same run

The screenshots show the human turn progressed substantially beyond startup:

1. live reasoning text streams incrementally;
2. process presentation remains visible while Vex reasons;
3. Vex emits a GitHub capability request;
4. an invalid request is surfaced as **invalid request**, not fabricated as success;
5. Vex reasons about the rejection and adjusts;
6. corrected GitHub repository `describe` succeeds;
7. Vex continues reasoning using the returned result;
8. GitHub README read is requested and succeeds;
9. Vex continues reasoning after the README result;
10. ordinary final Vex response bubble never appears before the run bound ends.

Do not claim scroll ownership passed solely from screenshots. The screenshots show internal scrollable process containers, but Victor did not provide a separate pointer-behavior verdict in this turn.

## Current open circuit

Preserve every earlier proven edge. The remaining failure is downstream:

```text
human Send                         observed
-> live provider reasoning         observed
-> capability request              observed
-> capability refused/invalid      observed
-> provider reasons again          observed
-> corrected capability request    observed
-> capability RETURNED             observed
-> next capability read            observed
-> capability RETURNED             observed
-> provider continues reasoning    observed
-> final assistant content         NOT OBSERVED
-> canonical response commit       NOT PROVED
-> TURN_COMPLETED / TURN_FAILED     NOT OBSERVED BY A009
-> final browser Vex bubble         NOT OBSERVED
```

This is now the foreground edge.

## Exact source trace for the next instance

### A. Canonical turn owner

Read:

`src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt`

Trace, in order:

- `performWithHooks(...)`
- first/new-turn runtime admission
- capability loop around `invokeProviderStage(...)`
- `stage.capabilityRequest`
- `VexLifeCapabilityBridge.perform(...)`
- `exchanges += exchange`
- `sameRuntime()`
- provider re-invocation with:
  `base.materialization.messages + exchangeMessages()`
- transition where `capabilityRequest == null` and `sourceReturnRequest == null`
- formation of `FinalProviderTurn`
- response formation
- context recording
- `commitCompletedTurn(...)`
- `VexLogger.Fact.TurnCompleted`
- failure path / `TurnFailed` receipt

Important: screenshots prove multiple capability stages occurred. Do not assume the model ever reached the `FinalProviderTurn` branch. Establish it.

### B. Capability owner

Read:

`src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityBridge.kt`

Trace:

- request parse;
- request handle/action normalization/admission;
- invalid request result semantics;
- successful `github.describe`;
- successful `github.read`;
- exact result message added back to provider context;
- request-number and exchange-token limits.

Do not "fix" an invalid request by silently coercing unsupported action names unless the capability contract actually owns that normalization. The current UI truthfully showed the invalid request and Vex corrected itself.

### C. Service streaming projection

Read:

`src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt`

Specifically `handleStreamingSend(...)`:

```text
DirectCompanionTurn progress
-> bounded projection queue
-> REASONING_DELTA / REACH / CONTENT_DELTA
-> canonical executor result
-> terminal COMPLETED or FAILED
-> [DONE]
```

The browser socket is an observer only. Do not put provider execution back under browser backpressure.

Establish whether the canonical executor ever returned in the failed lived run.

### D. Browser presentation

Read:

`src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt`

Trace:

- `send()`
- `readEventStream(...)`
- REASONING_DELTA
- REACH
- CONTENT_DELTA
- COMPLETED
- `completeCanonicalSend(result, streamed)`
- durable `loadConversation(false)` + `loadContext()`
- pending/live-process cleanup
- `render()`
- `reconcileSendOutcome(pending)` if streaming ends ambiguously

Current screenshots show reasoning and REACH projection working. Do not rewrite those pieces to solve a missing final response unless evidence places the break there.

### E. Chronology decides the owner

Read the Home chronology/failure receipt for the exact A009 turn. Required facts:

```text
SEND_ACCEPTED
PROVIDER_INVOCATION_REQUESTED
CAPABILITY_REQUEST_OBSERVED
CAPABILITY_RESULT_RETURNED
TURN_COMPLETED
TURN_FAILED
```

Decision:

```text
no TURN_COMPLETED and no TURN_FAILED
-> canonical turn/provider-capability continuation is still executing/stuck;
   investigate DirectCompanionTurn/provider stage first.

TURN_FAILED exists
-> use exact failureCode/failure receipt;
   do not infer from presentation.

TURN_COMPLETED exists but browser never shows final bubble
-> canonical turn is good;
   investigate LocalService terminal SSE / browser completeCanonicalSend / durable refresh.
```

This decision must precede mutation.

## Presentation behavior already worth preserving

Do not casually regress:

- reasoning streams live instead of arriving as one late block;
- capability request/result steps are human-readable;
- invalid capability request is visible as a real refusal/error;
- later successful GitHub capability results are visible;
- stable live-process DOM / scroll-ownership repair from #146 remains in the carried tree;
- raw reasoning remains session-scoped only, not cross-restart persisted;
- SSE projection is isolated from provider ingestion by a bounded observer queue.

## Tool/foundational orientation is NOT the current blocker

The capability frame already gives Vex:
- reachable handles;
- purposes;
- action examples;
- exact request/result grammar;
- "nothing is retrieved until a real result";
- bounded request allowance.

There remains a later teaching/orientation improvement: make illustrative examples vs actual current Product/source relationships clearer so Vex does not overthink "is this repo ours?" But do not mix that into the current final-turn closure defect.

Dream/retainment and clear-active-context-window work remain later, after communication is stable.

## What not to do

- do not use Local Source Workspace unless a later exact effect actually requires it;
- do not replay A001-A006;
- do not call #136 an accepted floor;
- do not broad-copy/revert predecessor trees;
- do not rewrite startup/runtime now that A009 proves it current;
- do not add cross-session raw reasoning persistence;
- do not let browser/presentation own canonical turn completion;
- do not replace a runtime inside one already-started human turn;
- do not infer "no Send" from A009 `WAITING_FOR_FIRST_TURN`;
- do not infer final-output bug location from screenshots alone.

## Fresh-instance objective

The next instance owes one result:

**Find the first missing owner-return edge after the last successful capability result and close the turn so the canonical answer commits, terminal truth reaches the browser, and the ordinary final Vex bubble appears — without regressing the now-proven startup/runtime/reasoning/capability behavior.**

After that passes lived proof, proceed to:
1. live scroll/pointer ownership;
2. same-session View Process after completion;
3. capability request/result/continuation proof;
4. fresh second Send / next-loop proof;
5. only then merge-worthiness review.
