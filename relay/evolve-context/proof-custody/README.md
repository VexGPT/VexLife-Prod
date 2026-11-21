# Vex EvolveContext proof / runnable custody

`[VXG RealForever]`

This branch is the durable sibling custody lane for EvolveContext runnables and RETURN evidence.

It deliberately does **not** own Product implementation. Product code lives in PR #504 (and successors).
Architecture intent lives in PR #500.

Each attempt adds one append-only attempt directory containing:
- the exact runnable ZIP that Victor was asked to execute;
- the exact RETURN ZIP received from that execution;
- a manifest binding both ZIPs to the implementation commit and observed outcome.

A failed attempt stays here. A successor does not rewrite it.

## Current lineage

- A002 implementation head: `a260b5f21a036aa86f8aa9c38b60dfa3390fed51`
- A002 runnable SHA-256: `0900ead88f16664e1e8a6f0762c26421007344dab0cba8be4b8c392eca803702`
- A002 RETURN SHA-256: `9b618854d1e8af2dabb05939d226cb77256166c3ddad03b57df397a660de6945`
- A002 outcome: focused JVM source compiled; treatment run returned nonzero; 1/5 focused tests failed.
- root cause identified afterward: source-return origin was not preserved through a later same-turn capability re-verification stage.

## Next attempt

- A003 implementation head: `f9a37bd8931d441760c19428ce3f6925e83d7d22`
- A003 runner SHA-256: `4d7c8264d5a95706fc20d84774670ed440c7e046b130f6b3da8245e5faf57319`
- A003 state: `RUNNER_FORMED_AWAITING_RETURN`
- A003 change: preserve source-return origin across same-turn capability re-verification and currentize onto the newest #503 proof head.

Future runners should be downloaded from this branch/PR by exact path + digest rather than regenerated from chat.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][PROOF-CUSTODY] -->
