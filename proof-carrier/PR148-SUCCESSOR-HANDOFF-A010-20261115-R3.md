[VXG RealForever]

# PR148 Successor Handoff R3 — Failure/Reconciliation Repair + A010

## Current Product

Repository: `VexGPT/VexLife-Prod`  
PR: #148  
Branch: `VXG-111526-vexlife-clean-convergence-a001`  
Head: `fd270a550ab8f37071d71144ee1241469db97c3b`  
Tree: `c4092271ccc4549290e5cb260a381b6f6644fefb`  
Base: `035335a34d4220d6d3242c088a1a9667c29f135f`  
State: DRAFT / HOLD.

## Enter

Read:
1. `AGENTS.md`
2. `docs/CULTURE.md`
3. `docs/foundation/INDEX.md`
4. VexArchitecture live demonstration
5. PR #148 current comments
6. bundled `ROOT-CAUSE-AND-REPAIR-A010.md`

Do not reconstruct from #136 labels. Follow source owner -> exact returned truth -> next edge.

## Stable floor to preserve

A009 proved:
- exact source/build/tests/private image;
- native Setup;
- exact Product CURRENT;
- certified 27B active.

Lived screenshots proved:
- live reasoning works;
- capability request/result presentation works;
- invalid request truth works;
- corrected GitHub describe works;
- README read works;
- provider continues after returned capability truth.

Do not reopen startup/runtime from the current final-output defect.

## Runtime repair already carried

```text
PROCESS/SUBSTRATE CURRENTNESS != GENERATION READINESS
```

At safe Setup/new-turn boundary:
- AVAILABLE -> reuse;
- UNAVAILABLE -> exact-owner recovery;
- UNKNOWN -> hold/preserve.

Inside one already-started turn:
- same runtime receipt required;
- no readiness-driven runtime swap.

Preserve.

## Final-output / failure presentation root cause

Old browser sequence:

```text
stream fault
-> erase reasoning/reaches/process
-> then reconcile canonical outcome
```

Terminal failure additionally did:

```text
retrySameSendAttemptAllowed=false
-> ignore exactNextSafeRoute
-> clear pending
-> copy original prompt into composer
```

Canonical meaning was actually:
`old idempotency key cannot replay; form a NEW Send attempt`.

## Repair now on Product head fd270a55

```text
stream fault
-> freeze/snapshot process
-> reconcile canonical owner

COMPLETED
-> attach snapshot to completed response

FAILED
-> session-only failed attempt retains process + failure route
-> Try Again creates NEW send.<uuid>

UNRESOLVED
-> same pending key + process remain
-> exact reconciliation retry does not reset process
```

No durable raw reasoning persistence added.

Source commits:
- `4a0f5ff3...` preserve failed process/new-attempt recovery
- `c07e77d2...` failure continuity regression guards
- `fef5efba...` preserve process through exact unresolved retry
- `fd270a55...` exact-retry reset guard

## Exact source owners to preserve

- `DirectCompanionTurn.kt`: canonical turn/failure route
- `VexLifeCapabilityBridge.kt`: request/result owner
- `VexLifeProdLocalService.kt`: terminal SSE projection
- `VexLifeWebP1RuntimeArtifacts.kt`: session presentation/reconciliation
- `DirectPromptContext.kt`: model-facing capability frame
- `VexLifeReachDefinitions.kt`: actual capability action scope

## Remaining unknown: canonical trigger of A009 long GitHub turn

Strong evidence, not certification:
- user intent targeted PR #141;
- github capability only supports `describe/tree/read`;
- invalid request consumed one request;
- corrected describe and README read consumed more;
- max four requests.

Do not increase limits or add PR capability until chronology says this caused the terminal edge.

## A010

A010 source-preflights exact head `fd270a55...` and runs native Product.

It retains content-free chronology + failure receipt summary in RETURN.

Carrier:
- runner commit `e7ad6b5cdad17b0fe5d6a9d61f9c9a766315c706`
- runner blob `a7a8a6b58ef170624b7fad4b7f3acf8ecb49f7c8`
- payload blob `aa8a61ba20de3527feac729cce46927bb9d35555`
- former blob `dcbf7bff56a1f8593823c8f8a982bda405a9ee04`

Lived test:
- reproduce the exact GitHub-heavy/failure scenario;
- if terminal failure occurs, verify process remains + Try Again appears;
- Try Again must not erase prior process and must mint a new Send;
- upload canonical A010 RETURN.

## After A010

Chronology decides next owner:

```text
TURN_FAILED
-> exact failureCode / failure receipt owns next investigation

no terminal fact
-> inspect last provider invocation + capability request number

TURN_COMPLETED
-> canonical turn is good; inspect service/browser terminal delivery only
```

Do not infer from screenshots once A010 return exists.
