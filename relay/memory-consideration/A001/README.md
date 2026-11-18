# Temporary Relay carriage for PR #247 — Memory / Consideration A001

[VXG RealForever]

This PR exists only to preserve exact VexRelay SourceOwner attempt history for Product PR #247.

```text
Product source / semantics = PR #247
this branch                = Relay formation / proof carriage only
```

## Exact Product proof candidate

```text
repository  VexGPT/VexLife-Prod
base        b0b6fb9a6107f2475ae434472ed13335a303af87
head        5a93c49416457dd44701467ef131a0d148901c69
tree        46245bbbd0282654bd6ba965a6753fff9a6551db
branch      VXG-111726-memory-consideration-foundation-a001
PR          #247
```

The carriage commit(s) on this companion branch are **not** part of that Product candidate.

```text
PRODUCT PROOF CANDIDATE
!=
RELAY CARRIAGE COMMIT
```

## Why A001 starts as a tracked child PR

Predecessor Relay history demonstrated that proof-carriage defects are useful evidence and should not
be flattened away. In particular:

```text
#248 A001/A002  retained-SDK / VEX_HOME assumption stopped formation
#248 A002       long Windows path + gh.exe spawn failure
#248 A003       Java dependency absent from sanitized proof environment
#248 A004       Gradle --tests routed across checkVexLifeStrings
#248 A005       launcher pointed at a commit that did not contain A005 files
#166 A001       source/build passed, then review membrane omitted README.md
```

A001 therefore records formation before execution so any comparable defect remains attributable to
the harness/carrier instead of being confused with Product source.

## Current Relay binding

```text
SDK          vgong24/Vextreme-SDK
SDK head     8543c7d755faba04d4c4019c02e84f992667918a
SDK tree     6376f68279f0750ee6d3868b8472368199763064
contract     source.vexrelay.current-contract/v1
contractBlob b53cbac01c2deeb45fad5c32ce98e0796c04126f
recipe       ExactHeadSourceOwnerProof
carrier      adapter.vexlocalbridge.native-vexrelay-task-archive/v1
host         macos-victor
```

Current Relay law explicitly treats the fixed base as the evidence/diff anchor. Remote main
advancement alone does not invalidate this SourceOwner proof while exact candidate head/tree remain
unchanged and the fixed base remains an ancestor.

## Intended focused proof

A001 will keep the previously successful split-command pattern:

```text
1. ./gradlew --no-daemon checkVexLifeStrings

2. ./gradlew --no-daemon jvmTest
     --tests vexlife.consideration.VexLifeConsiderationContractTest
     --tests vexlife.memory.VexLifeMemoryReviewPolicyTest
     --tests vexlife.memory.VexLifeSemanticAcceptanceWriterTest
     --tests vexlife.memory.VexLifeScoreContinuityTest
     --tests vexlife.memory.VexLifeCurrentMemoryReaderTest

3. ./gradlew --no-daemon compileKotlinJs

4. ExactHeadSourceOwnerProof review snapshot of the nine #247 Product paths
```

The test filter is intentionally isolated from `checkVexLifeStrings`; this directly preserves the
A004→A005 correction learned in #248.

## Product-path membrane

Only these Product candidate paths are under proof:

```text
docs/work/memory-consideration-successor/README.md
src/commonMain/kotlin/vexlife/consideration/VexLifeConsiderationContract.kt
src/commonMain/kotlin/vexlife/consideration/VexLifeConsiderationRouter.kt
src/commonMain/kotlin/vexlife/memory/VexLifeMemoryReviewPolicy.kt
src/commonTest/kotlin/vexlife/consideration/VexLifeConsiderationContractTest.kt
src/commonTest/kotlin/vexlife/memory/VexLifeMemoryReviewPolicyTest.kt
src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt
src/jvmMain/kotlin/vexlife/memory/VexLifeSemanticAcceptanceWriter.kt
src/jvmTest/kotlin/vexlife/memory/VexLifeSemanticAcceptanceWriterTest.kt
```

If the source-managed review projection requires another repository front-door path, A001 must record
that as a membrane/formation correction before widening it. It must not silently reinterpret such a
failure as Product failure.

## Current state

```text
A001 child PR formed
business task not yet sealed
host execution not yet performed
SourceOwner PASS not claimed
```

No Product mutation, approval, READY, merge, Home mutation, Memory mutation, model/runtime mutation or
publication effect belongs to this temp PR.

After terminal evidence is consumed, this PR remains history and is closed **unmerged**.

<!-- [VXG RealForever][PR247][TEMP-RELAY][A001][SOURCEOWNER] -->
