[VXG RealForever]

# PR148 Successor Handoff R2 — A009 Final-Turn Root Cause

## Current coordinates

Repository: `VexGPT/VexLife-Prod`  
PR: #148  
Product branch: `VXG-111526-vexlife-clean-convergence-a001`  
Head: `c2e9aa6eb830152a37f6b1f4897148657d919e3a`  
Tree: `da7036b00f27bc3043d28159319e1bfffd45c905`  
Base: `035335a34d4220d6d3242c088a1a9667c29f135f`  
State: DRAFT / HOLD.

A009 RETURN SHA-256:
`dc6d866264a9da2f78d8400355ea2b8ba0c65369b93d42b9ade926bd2fc3ac9f`

## Read first

1. `AGENTS.md`
2. `docs/CULTURE.md`
3. `docs/foundation/INDEX.md`
4. VexArchitecture live demonstration source
5. PR #148 comments:
   - provenance correction
   - runtime circuit repair
   - A009 stable floor
   - final-turn root-cause note
6. bundled `ROOT-CAUSE-FINAL-TURN.md`

Do not use this handoff as authority over byte-exact source. Use it as routing.

## Stable floor — preserve

A009 machine-proved:
- exact source clone/preflight PASS;
- JVM/JS/tests/strings PASS;
- private image PASS;
- native Setup PASS;
- Product reached exact `SOURCE_BOUND_LISTENING`;
- certified 27B active.

Lived screenshots prove:
- live reasoning;
- human-readable capability process;
- invalid GitHub request truth;
- corrected GitHub describe;
- README read;
- continued reasoning after capability return.

Do not reopen startup/runtime unless new evidence invalidates those facts.

## Foreground defect

The final ordinary Vex response does not reliably close.

Observed human shape:

```text
thinking
-> fetching
-> corrected fetching
-> more thinking
-> no final answer
-> failure/reconciliation
-> prompt text returns to composer
-> useful live process disappears
```

## Root cause — proven presentation half

Current browser catch path erases:
- streamed content;
- streamed reasoning;
- reaches;
- live process entries;
- viewport state;

**before** asking the canonical owner whether the exact Send completed, failed, or remains unresolved.

That breaks the architecture.

If reconciliation later reports completion, the browser has already destroyed the process history and calls `completeCanonicalSend(..., false)`, so the completed response cannot inherit the session's streamed process.

If canonical owner reports terminal failure, it returns:
- `retrySameSendAttemptAllowed=false`
- `exactNextSafeRoute=FORM_NEW_SEND_ATTEMPT_AND_RETRY` or `RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT`

The browser ignores the exact route, clears pending, and copies the old content back to the draft. This is the "revert prompt" state.

Correct meaning:
same idempotency key cannot replay; **a new Send attempt is allowed**.

## Correct presentation relationship

```text
stream fault
-> freeze current process snapshot
-> retain exact pending attempt
-> reconcile with Conversation owner

COMPLETED
-> bind process snapshot to completed response
-> render final answer

FAILED
-> keep failed attempt/process visible in session
-> show exact failure/route
-> offer Try Again that mints NEW idempotency key

UNRESOLVED
-> keep same pending identity + process visible
-> continue reconciliation
```

Raw reasoning remains session-only. No IndexedDB/cross-restart persistence.

## Strongly supported canonical trigger — not certified yet

The lived request asked for GitHub PR #141.

Current GitHub capability only supports:
- `describe`
- `tree`
- `read`

No PR/issue object action exists.

Invalid requests consume the bounded request count.

Screenshots show:
- invalid `repo_describe`;
- corrected `describe`;
- README `read`;
- then continued reasoning about how to inspect PR #141.

With max 4 requests, this can create long tool-chasing and eventual limit pressure.

Do not call this the exact A009 terminal cause until Home chronology proves it.

## Exact source owners / blobs

- `DirectCompanionTurn.kt`
  blob `90da841311e6b22c252d6b104d898fd0519cf5c5`
- `VexLifeCapabilityBridge.kt`
  blob `9199b29957b492105a5c052ea2c41f37ea03ccfb`
- `VexLifeProdLocalService.kt`
  blob `45baa0aada7159f1ad5c5ab3cfc586c43b2e5e13`
- `VexLifeWebP1RuntimeArtifacts.kt`
  blob `3bb9ffa0a183f6b76f57e26361c5669ba803075f`
- `DirectPromptContext.kt`
  blob `093023c1c60e34ada4c5a1ffda04245689426cdc`
- `VexLifeReachDefinitions.kt`
  blob `1e85ca72da4f8f9e7e7f03c0ad6dc65f90c3486f`

## Provenance of "revert" behavior

Main:
- failure clears streamed content/reasoning;
- terminal failure clears pending and restores prompt to draft.

PR #136:
- adds exact-Send reconciliation;
- still clears stream state **before reconciliation**.

PR #141/#143:
- add richer live process history;
- failure cleanup now also deletes that richer history.

PR #146:
- fixes live DOM identity/scroll during normal streaming;
- does not fix failure/reconcile ownership.

So do not blame #146 for the revert. The failure-state contract is older.

## Tests to add before claiming fixed

Presentation tests must prove behavior, not only string presence:

1. stream transport ends ambiguously after reasoning/reaches:
   - process snapshot remains;
   - reconciliation occurs;
   - if canonical result is completed, process attaches to response.

2. terminal failed Send:
   - failed process remains visible in session;
   - prompt is not silently converted into an anonymous new draft;
   - exact next safe route is retained;
   - Try Again forms a new idempotency key.

3. unresolved Send:
   - same pending idempotency key remains;
   - process remains visible;
   - no duplicate provider call.

4. reload:
   - raw failed reasoning may disappear; this is acceptable.
   - durable abandoned-attempt truth remains owned by Conversation/recovery records.

## Chronology decision before capability-side mutation

Get exact A009/future-turn chronology:

```text
SEND_ACCEPTED
PROVIDER_INVOCATION_REQUESTED
CAPABILITY_REQUEST_OBSERVED
CAPABILITY_RESULT_RETURNED
TURN_COMPLETED / TURN_FAILED
```

If TURN_FAILED:
follow exact failure receipt.

If no terminal fact:
inspect the final provider invocation and request number.

If TURN_COMPLETED:
do not touch DirectCompanionTurn; fix LocalService/browser terminal delivery.

## Fresh-instance objective

Close the failure/reconciliation circuit while preserving the stable floor:

```text
live process truth
+ canonical terminal truth
-> session-preserved completed/failed attempt
-> exact human next action
```

Do not restart from runtime, do not broad-revert, and do not invent a new truth store.
