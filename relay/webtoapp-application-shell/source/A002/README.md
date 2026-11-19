# PR #396 WebToApp application-shell SourceOwner A002

[VXG RealForever]

A001 remains immutable superseded-before-execution history for Product head
`49731f85c65d0e2c15c63c6781b3bf3c3087129e`.

A002 binds the corrected exact Product candidate after fresh lifecycle review.

## Exact candidate

```text
base   fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head   49670f1811683c0f8a426012ed58515383620bf0
tree   8767834d611f43462d6d75a0f4df5f00304f3844
branch VXG-111926-chatgpt-webtoapp-application-shell-a001
paths  19
```

## Corrections since A001 binding

```text
DESIRED APPLICATION SOURCE
!= EXACT CURRENTLY-OWNED PRODUCT SOURCE

endpoint/readiness unavailable
!= ownership unavailable
```

Mac Product Quit now:
- lets the lifetime owner resolve the exact current source/PID from its own receipt/launchd/typed
  ownership rather than accepting the desired source from presentation;
- may retire an exact owned same-Home service even when its endpoint is not ready;
- still refuses another Home's exact service, legacy-orphan authority, or UNKNOWN ownership.

## Route

```text
source.vexrelay.current-contract/v1
ExactHeadSourceOwnerProof
adapter.vexlocalbridge.native-vexrelay-task-archive/v1
macos-victor
Local Workspace NOT USED
GitHub Actions NOT USED
human ZIP carriage NOT USED
```

The driver consumes both canonical return layers locally and emits one compact receipt.

Source proof still cannot satisfy the hard merge gate:

```text
built Mac VexLife application lived run PASS
+ built Windows VexLife application lived run PASS
= required before merge consideration
```

<!-- [VXG RealForever][PR396][PR397][SOURCEOWNER][A002] -->
