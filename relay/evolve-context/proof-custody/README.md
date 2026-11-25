# Vex EvolveContext proof / runnable custody

`[VXG RealForever]`

This branch is the durable sibling custody lane for EvolveContext runnables and RETURN evidence.

It deliberately does **not** own Product implementation. Product code lives in PR #504 (and successors).
Architecture intent lives in PR #500.

Each attempt adds one append-only attempt directory containing:
- the exact runnable ZIP that Victor was asked to execute;
- a manifest binding the runnable to the implementation commit and observed outcome;
- exact RETURN ZIP bytes when the connector can verify and retain them safely, otherwise the verified archive SHA-256 plus extracted source/test evidence without pretending the binary was preserved.

A failed attempt stays here. A successor does not rewrite it.

## Current lineage

- A002 implementation head: `a260b5f21a036aa86f8aa9c38b60dfa3390fed51`
- A002 runnable SHA-256: `0900ead88f16664e1e8a6f0762c26421007344dab0cba8be4b8c392eca803702`
- A002 RETURN SHA-256: `9b618854d1e8af2dabb05939d226cb77256166c3ddad03b57df397a660de6945`
- A002 outcome: source compiled; 5 focused tests ran; treatment returned nonzero; 1 focused test failed.
- A002 diagnosis: source-return origin was lost across later same-turn capability re-verification.

- A003 implementation head: `f9a37bd8931d441760c19428ce3f6925e83d7d22`
- A003 runner SHA-256: `4d7c8264d5a95706fc20d84774670ed440c7e046b130f6b3da8245e5faf57319`
- A003 RETURN SHA-256: `294f2422c96811cb6ef5f7aabe78f55ba3711ec3cc6be6fed95b7f8311ad1ad7` (verified user-returned archive; extracted evidence retained under A003)
- A003 outcome: compilation PASS; 4/4 host cue/source tests PASS; treatment projection stops at ordinal 21 with `ADMISSION_FAILED` before provider-call assertions.
- A003 next edge: record exact failed-turn cause/provider stage before another semantic change.

## A004

- implementation head: `6a793ea6dcd438dfb9be307ed266b38f420cf986`
- runner SHA-256: `990b066919a7c1aa84830ffbd7a8978f822b9ee07e507fdb1fe2ac26c5b851ea`
- RETURN SHA-256: `72fdacb397d7edf2485538fb496cfe5741bfda40d2b26bb0be882224ef6fe9dc`
- state: `FAILED_SEMANTIC_BOUNDARY_PROVED`
- semantic delta: none
- result: host source reached two provider stages; durable turn evidence then rejected the Source Return-shaped receipt because no provider Source Return trace exists.
- next edge: split host Context Continuation receipt/projection identity from provider Source Return.

Future runners should be downloaded from this branch/PR by exact path + digest rather than regenerated from chat.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][PROOF-CUSTODY] -->


## A005

- implementation head: `05253005fe65137b66fd395e38f64714b9c0235a`
- upstream proof base: `d6fd40b491355534ce58c59fc93f1a5ce8df9920`
- runner SHA-256: `8f475ad270acf2ab3b98e9682d4056319995bd93c39fe3d6e112c9a68cb2bb5f`
- runner bytes: `2490`
- runner custody commit: `860e1632349c3df3e759980d031468bc9af3c67f`
- byte verification: exact Base64 match after GitHub re-fetch
- semantic delta: host Context Continuation now has its own prompt/evidence identity; provider Source Return remains provider-requested and trace-bound.
- RETURN SHA-256: `48b930298e40853361d12877c827f41c8237337b8a412cc6086ca1ddd2d76936`
- state: `FOCUSED_PASS_FULL_REGRESSION_ONE_STALE_EXPECTATION`
- result: Stage-A semantic split focused proof PASS; full JVM 998 tests / 1 stale future-schema fixture failure.
- next edge: A006 test-only v10 -> v11 correction, then rerun full regression.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][A005] -->


## A006

- merged Product main: `94d6b7496507a673008e96949ea2e13f55163506`
- Foundation: `v0.6.19`, body `08b5b3c6f1fbbe71be84c8873a03a184f17a5b3051ed7131ebc381e74a0c9d0c`
- implementation head: `64d830f3afbc4429a651aa707d4078df740bbb11`
- semantic delta from A005: none; source naming currentized to `DirectConversationContextProjection`
- compatibility delta: unsupported-future prompt receipt test fixture `v10 -> v11`
- runner SHA-256: `50a6babeaea2276755c790f112cc5347a9445e9852046afaea359f4fcb7df10f`
- runner bytes: `2679`
- runner custody commit: `0c6a0563e192da10c52ffbb3287f3de20290b02b`
- exact Git blob verification: `fbb3de56ce864ee47d1eb72a7100e15308c90c8f` local == GitHub
- bootstrap commit: `ba4ab52253330c1b78d41fde72c1fbe6f3809802`
- state: `RUNNER_FORMED_AWAITING_RETURN`

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][A006] -->
