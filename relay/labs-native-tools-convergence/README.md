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
