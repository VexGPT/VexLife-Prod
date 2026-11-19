# PR #393 convergence proof companion

[VXG RealForever]

DO-NOT-MERGE proof carriage for the integrated Vex Labs + provider-native tools / layered-context candidate.

## Exact Product candidate

```text
base 146b0d482517201b85990a9d7b0b9ebf04075fd9
head d3c3da57d6042464f345ed0c4a98405a4f6fc884
tree 624e5dccdfc06c22bb1451479eaa2a15671a1533
changed paths 45
```

## Provenance

```text
#381 de4801e54d5b53e722b271d35135723c769660a0
  SourceOwner PASS
  Independent Assurance CLEAR

#383 0cd7b88a8b15b2fee6bb748ea5787d57da4ac91e
  SourceOwner PASS
  Independent Assurance CLEAR

accepted main / #338 merge
146b0d482517201b85990a9d7b0b9ebf04075fd9
```

Composition is 10 exact #381 blobs + 28 exact #383 blobs + seven explicit convergence seams.

## Convergence law

```text
Lab/turn policy
  -> whether effect may occur

provider-native representation
  -> how known route is expressed

Bridge / Source Return / rightful source owner
  -> admission re-check + performed/result truth
```

Stable native tool schemas remain declared even when Lab policy holds the effect. Held-by-turn-policy remains distinct from provider-bridge hold and request-budget exhaustion.

## Attempt progression

```text
A001  compile PASS
      853 JVM tests / 1 failure
      DirectCompanionConnectionTest.windowsRealizationMayServeTheSameProductModelThroughItsOwnExactEndpoint
      root cause: old-base child blob displaced accepted #338/main parent realization/service behavior

A002  CURRENT
      DirectCompanionConnection rebuilt as current-main + #383
      VexLifeProdLocalService rebuilt as current-main + #381
      DirectCompanionTurn re-audited as current-main + #381 + #383 and unchanged from A001

A003  LIVED PRESSURE COMPLETED / SOURCE CORRECTION FORMED
      exact 4B completed 15 turns; rollover at turn 10; successor mode from turn 11
      provider reported finish_reason=tool_calls on turns 13/14
      no consumable native tool call reached Capability Bridge
      four source-dependent post-rollover probes failed; absence remained unresolved
      correction on #393 8abb3cc86af1b5de5840f8ae59c2bd74bf49ef01
        Qwen 4B -> VEXLIFE_CONTROL_TEXT compatibility representation
        native OpenAI tool_calls finish with no consumable call -> fail closed
      fresh SourceOwner + fresh Assurance required before lived A004
```

## Current proof

```text
source.vexrelay.current-contract/v1
ExactHeadSourceOwnerProof
adapter.vexlocalbridge.native-vexrelay-task-archive/v1
macos-victor
Local Workspace NOT USED
```

A001 runs:
- full JVM tests;
- Kotlin/JS main + test compilation;
- strings;
- generated VexWeb P1.

PASS advances only to fresh Independent Assurance on the exact integrated head.

After Assurance CLEAR, the next lived pressure target is the original real 4B / 15-turn continuity/Foundation scenario.

READY / merge remain held.

<!-- [VXG RealForever][PR393][TEMP-RELAY][DO-NOT-MERGE] -->


A004  SOURCEOWNER REVIEW-ONLY RECOVERY CURRENT
      A003 exact-head whole-source proof command PASS is preserved
      A003 stopped only at REVIEW_SNAPSHOT because the membrane listed 45 paths
      corrected candidate has 46 paths; newly included:
        src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt
      Product head/tree unchanged:
        8abb3cc86af1b5de5840f8ae59c2bd74bf49ef01
        98d9e7aba5b1a69f7a8c75e87ffb7cb1a4b4e8cb
      A004 proofCommands = []
      current effect = missing exact-head review snapshot only
      PASS advances to fresh Independent Assurance


A005  SOURCEOWNER REVIEW-ONLY RECOVERY CURRENT
      A004 failed during task formation before any Product/proof effect:
        MANIFEST_STRING_ARRAY:authority.allowedExecutables
      current Relay manifest requires allowedExecutables to be non-empty
      A005 keeps /bin/bash in the allowlist as contract metadata
      proofCommands remains []
      therefore the completed A003 Gradle proof is still not replayed
      Product head/tree remain unchanged:
        8abb3cc86af1b5de5840f8ae59c2bd74bf49ef01
        98d9e7aba5b1a69f7a8c75e87ffb7cb1a4b4e8cb
      current effect = missing exact-head review snapshot only


A006  FRESH SOURCEOWNER AFTER ASSURANCE CORRECTION
      prior A005 exact-head review PASS consumed
      fresh Assurance on 8abb3cc8... found one semantic prompt-truth mismatch:
        shipped Qwen 4B fallback text still claimed "imported model" / "no native tool channel"
      bounded Product correction:
        DirectPromptContext -> truthful representation wording
        DirectPromptCurrentStateTest -> regression against false reason
      current Product:
        head 1059d6bbaf26ef6797c010901dc1eb5caf0725f3
        tree 1e9c2b2ed1e88a9e88054ecc7389a3e7757c4d76
        changed paths 46
      A006 reruns the whole-source threshold because Product bytes changed
      PASS advances only to fresh Independent Assurance on the exact new head


A007  FRESH SOURCEOWNER AFTER TEST-IMPORT CORRECTION
      A006 outer carrier PASS / inner SAFE_FAILURE consumed
      exact candidate compiled JVM main; test compilation stopped at:
        DirectPromptCurrentStateTest.kt unresolved reference assertFalse
      cause:
        fresh Assurance regression used assertFalse without importing kotlin.test.assertFalse
      bounded correction:
        one test import only
        Product runtime semantics unchanged
      current Product:
        head c646f8d10cd99ed4891f56ec2031269762cd38ae
        tree 2c402f16fe891a1fe66d5f0f25cc8f20ca135fc1
        changed paths 46
      A007 reruns the whole-source threshold
      PASS advances only to fresh Independent Assurance on the exact new head


A008  DOC-ONLY SOURCEOWNER REVIEW CURRENT
      A007 outer/inner PASS consumed:
        whole-source build PASS
        46-path review PASS
      fresh Assurance on c646f8d1... found one stale Memory Depth sentence:
        fallback was described as only for providers that genuinely lack native tools
      doc-only correction:
        fallback is valid when native tools are absent OR the exact native-call path is not lived-qualified
      executable source unchanged from A007
      current Product:
        head 9182fb01353d4097f1935c48d0d862921fa2c698
        tree 2b73f12058c5757fe99b84b21ad761e205d4fea2
        changed paths 46
      A008 proofCommands = []
      current effect = exact-head review/currentness only
      PASS advances to fresh Independent Assurance
