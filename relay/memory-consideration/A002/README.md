# Temporary Relay carriage for PR #247 — Memory / Consideration A002

[VXG RealForever]

A001 remains immutable history in this temp PR.

## A001 result

```text
outer VexLocalBridge
  sha256 ca79c43a15885c3be7cde261ccb5afeed93ea861970d17f43df7e4d8f8b36994
  disposition PASS

inner VexRelay
  sha256 7a441ca6d5de8035109cc959691567df88467566f87c70e4815a14cb62a25e54
  disposition SAFE_FAILURE

repository grounding PASS
checkVexLifeStrings PASS
full jvmTest reached
648 tests / 4 failed
```

All four failures were the new `VexLifeSemanticAcceptanceWriterTest` and shared one fixture defect:
the proposal supplied only `response.eventHash` while the existing Score source owner requires complete
REQUEST + RESPONSE pairs.

No writer semantic change was made to repair A001.

## Corrected Product candidate

```text
PR    #247
base  b0b6fb9a6107f2475ae434472ed13335a303af87
head  db0bda47aebb77e5df58e55545fa76eb9b42e4c4
tree  dbb07f55d86cdfaffc76391bf0be26eeb3564349
branch VXG-111726-memory-consideration-foundation-a001
```

The correction changes only:

```text
src/jvmTest/kotlin/vexlife/memory/VexLifeSemanticAcceptanceWriterTest.kt

single RESPONSE source hash
->
exact REQUEST + RESPONSE source pair
```

#254 was forward-currentized with a two-parent commit; A001 history was not rewritten.

## A002 stable formation bundle

```text
VexLife-Memory-Consideration-RelayFormation-A002--sha256-6098e35ad75ecd9dd5eaa0c83e000314d788d02f845270b711800e16e17272c7.zip
sha256 6098e35ad75ecd9dd5eaa0c83e000314d788d02f845270b711800e16e17272c7
bytes 8387
```

The bundle keeps the same source-owner proof:

1. `checkVexLifeStrings`
2. full `jvmTest`
3. `compileKotlinJs + compileTestKotlinJs`
4. exact nine-path `ExactHeadSourceOwnerProof` review snapshot

Attempt/package/request/nonce identities are fresh `a002`. Long-lived lane/work/claim identity remains
the same trajectory.

No Product mutation, Home/Memory/model mutation, publication, approval, READY or merge effect is
authorized by this temp proof.

A002 is not PASS until its canonical RETURN is consumed.

<!-- [VXG RealForever][PR247][PR254][TEMP-RELAY][A002] -->
