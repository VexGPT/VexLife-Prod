# A005 RETURN evidence

`[VXG RealForever]`

User-returned archive:
- filename: `RETURN-VEX-EVOLVECONTEXT-STAGE-A005-20261121T182836Z.zip`
- SHA-256: `48b930298e40853361d12877c827f41c8237337b8a412cc6086ca1ddd2d76936`
- bytes: `737097`
- formed at: `2026-11-21T18:29:40Z`

Source binding:

```text
expected_head=05253005fe65137b66fd395e38f64714b9c0235a
observed_head=05253005fe65137b66fd395e38f64714b9c0235a
branch=VXG-112126-vex-evolve-context-stage-a002
source clean before/after=true
```

Focused JVM gate:
- `compileKotlinJvm`: PASS
- `compileTestKotlinJvm`: PASS
- `VexLifeHostContextContinuationTest`: 5/5 PASS
- `ContinuityPressureAdapterTest`: 9/9 PASS, including hostContinuationTreatmentPutsOlderExactSourceInTheFirstProviderCall
- `DirectPromptContextTest`: 24/24 PASS
- `DirectTurnContextStoreTest`: 5/5 PASS
- focused gate exit: 0

This proves together:
- host Context Continuation has its own provider-facing identity;
- the old cobalt source is present in the treatment's first provider call;
- host continuation does not masquerade as provider Source Return;
- prompt receipt v10 materialization/reverification is coherent;
- durable turn context accepts host-continuation evidence without a Source Return trace;
- provider Source Return trace semantics remain independently covered.

Full JVM regression:
- 998 tests completed
- 1 failed
- failing test:
  `VexLifeProdLocalServiceTest.newerDurablePromptReceiptHoldsOnlyContextProjection`

Exact failure:
```text
expected HTTP 200 held projection
actual HTTP 500 PRODUCT_CONTRACT_SERIALIZATION_FAILED
```

Root cause:
the test intentionally writes a durable prompt receipt schema one version newer than Product supports.
Before A005 the current receipt was v9, so the fixture used v10. A005 lawfully made v10 current. The
fixture remained v10, so Product now treats it as readable and attempts to deserialize the intentionally
minimal object. The compatibility expectation is stale; Product behavior under the new schema is not
what failed.

Next edge A006:
- change that test fixture only from v10 to v11;
- rerun focused gate + full JVM regression;
- no EvolveContext semantic change.

<!-- [VXG RealForever][VEX-EVOLVECONTEXT][A005][RETURN-EVIDENCE] -->
