[VXG RealForever]

# PR148 Root Cause + Repair — Failed Output / Prompt Revert / Lost Thought History

## Exact current Product

- Repository: `VexGPT/VexLife-Prod`
- PR: `#148`
- Branch: `VXG-111526-vexlife-clean-convergence-a001`
- Head: `fd270a550ab8f37071d71144ee1241469db97c3b`
- Tree: `c4092271ccc4549290e5cb260a381b6f6644fefb`
- Base: `035335a34d4220d6d3242c088a1a9667c29f135f`
- State: DRAFT / HOLD

A009 prior lived evidence:
- RETURN SHA-256 `dc6d866264a9da2f78d8400355ea2b8ba0c65369b93d42b9ade926bd2fc3ac9f`
- source/build/private-image PASS
- native Setup -> Product CURRENT PASS
- live reasoning and GitHub capability work observed
- no final ordinary answer
- machine terminal: `SAFE_FAILURE / WAITING_FOR_FIRST_TURN`

## Proven root cause: presentation erased returned process truth before canonical owner outcome

Old browser failure order:

```text
stream error / terminal FAILED
-> clear streamedContent
-> clear streamedReasoning
-> clear liveReaches
-> clear liveProcessEntries
-> reset viewports
-> only then ask canonical Conversation owner to reconcile
```

That violates VexArchitecture:

```text
presentation observation
must not destroy its evidence
before rightful semantic owner returns terminal truth
```

If canonical reconciliation later said COMPLETED, the process the human had just watched was already gone.

## Proven root cause: exact retry semantics were collapsed into "put prompt back in the draft"

Canonical Conversation owner already returns:

```text
retrySameSendAttemptAllowed = false
exactNextSafeRoute =
  FORM_NEW_SEND_ATTEMPT_AND_RETRY
  or
  RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT
```

Canonical tests prove:
- same failed idempotency key must not replay;
- failed request is preserved as an abandoned attempt;
- a NEW Send identity may retry;
- if a prior completed head exists, the new attempt resumes from it.

Old browser behavior ignored `exactNextSafeRoute`:

```text
retryAllowed == false
-> state.pending = null
-> ui.draft.value = pending.content
-> "Send failed"
```

Thus:
`same attempt cannot replay`
was mis-presented as:
`human must manually start over`.

## Proven provenance

- merged main already cleared streamed content/reasoning on terminal failure;
- PR #136 added exact-Send reconciliation but still cleared stream state **before** reconciliation;
- PR #141/#143 added richer process history but inherited old failure cleanup;
- PR #146 fixed live DOM/scroll ownership during normal streaming but inherited the same failure-state semantics.

Therefore the revert/history-loss bug predates the rich process UI. It became more visible/destructive as the process projection became richer.

## Current repair on head fd270a55

Current browser relationship:

```text
stream fault
-> freezeLiveProcess()
-> snapshotLiveProcess()
-> preserve exact pending identity
-> reconcile with canonical owner
```

### Reconciliation returns COMPLETED

```text
canonical completed response
-> attach preserved process snapshot to responseMessageRef
-> refresh durable Conversation/Context
-> clear transient live state only after transfer
-> render final answer
```

### Canonical owner returns terminal FAILED

```text
failureCode + exactNextSafeRoute
+ process snapshot
+ original failed idempotency key
-> session-only failed-attempt projection
-> old key is retired
-> process/thought/reaches remain inspectable in this browser session
-> Try Again control appears
-> Try Again mints NEW send.<uuid>
```

The prompt is no longer silently restored to the draft.

### Outcome unresolved

```text
same pending idempotency key remains
+ live process remains
-> exact reconciliation retry
```

An exact retry does **not** reset the live process slot. Only a genuinely new Send does.

## Privacy/lifetime unchanged

No new durable reasoning store was introduced.

Raw process/reasoning:
- may remain in browser-session memory for completed/failed attempts;
- may disappear on reload/restart;
- is not written to Conversation/Memory/training/IndexedDB.

Durable owners remain:
- Conversation canonical events;
- abandoned-attempt/failure receipts;
- Context receipts;
- capability trace hashes/refs.

## Regression guards added

`src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt`

Now guards:
- terminal failure cannot use `ui.draft.value = pending.content`;
- reconciliation completion receives preserved `interruptedProcess`;
- terminal failed attempt stores `processEntries: interruptedProcess`;
- Try Again exists;
- Try Again mints a new Send identity;
- exact unresolved retry does not reset live process state;
- `exactNextSafeRoute` is retained in presentation code.

## Still-open canonical trigger

The presentation root cause is fixed independent of canonical trigger.

The exact reason the A009 GitHub-heavy turn failed to terminalize is still not proven.

Strongly supported hypothesis:
- intent was to inspect PR #141;
- current GitHub capability owns only repository `describe/tree/read`;
- no PR/issue object action exists;
- screenshots show one invalid action, then corrected repo describe, then README read, then continued attempts;
- invalid requests consume the bounded request number;
- max requests per turn is 4.

Possible outcomes:
- capability exhaustion;
- provider stage still running at A009 bound;
- later connection failure;
- another canonical failure.

Do not mutate capability limits/scope from inference alone.

## A010 resolves the remaining evidence gap

A010 is bound to the repaired head and performs full source preflight before native effects.

A010 RETURN additionally retains content-free chronology:
- `SEND_ACCEPTED`
- `PROVIDER_INVOCATION_REQUESTED`
- `CAPABILITY_REQUEST_OBSERVED`
- `CAPABILITY_RESULT_RETURNED`
- `TURN_COMPLETED`
- `TURN_FAILED`
- refs/states/request numbers;
- failure receipt summary including exact next safe route.

No message/response/page/reasoning content is retained by the proof.

## A010 immutable carrier

- runner commit: `e7ad6b5cdad17b0fe5d6a9d61f9c9a766315c706`
- runner blob: `a7a8a6b58ef170624b7fad4b7f3acf8ecb49f7c8`
- payload blob: `aa8a61ba20de3527feac729cce46927bb9d35555`
- former blob: `dcbf7bff56a1f8593823c8f8a982bda405a9ee04`
- candidate head: `fd270a550ab8f37071d71144ee1241469db97c3b`
- candidate tree: `c4092271ccc4549290e5cb260a381b6f6644fefb`

## Lived acceptance for this repair

Reproduce the GitHub-heavy/failure scenario.

If the turn fails:
- process/thought history must remain visible;
- prompt must not silently jump back into the draft;
- a failed-attempt projection must remain;
- Try Again must be available;
- Try Again must start a NEW Send identity.

If the stream ends ambiguously but canonical turn actually completed:
- final Vex answer must appear;
- same-session process history must transfer to the completed response.

Then consume A010 chronology to determine the canonical trigger.
