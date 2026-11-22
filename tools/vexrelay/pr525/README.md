# PR #525 Warm Boot runtime-continuity composition — SourceOwner companion

[VXG RealForever]

This directory owns only SourceOwner proof carriage for stacked Product PR #525.
It must not merge into Product source.

## Why #525 exists

Warm Boot lived #520 A002 reached exact #518 source and a canonical Mac application build, then
retired the prior #510-derived service and stopped because the ordinary #518/main replacement path
could not become source/PID-current.

The source relationship recovered from accepted predecessor work is:

```text
#507
  prior service retires
  -> Setup re-establishes exact runtime custody before replacement service starts

#510
  while Setup is already alive and holds the exact runtime
  -> form one short-lived exact handoff
  -> old service relinquishes only duplicate runtime-release duty
  -> same MLX receipt/PID survives service source rotation

#518
  replacement service
  -> background exact-prefix Prompt Preparation
  -> real Send may consume READY_REUSABLE only after canonical REQUEST
```

#525 composes #507 then #510 onto the exact #518 head. It does not replay either historical branch
tree or unrelated historical changes.

## Exact candidate

```text
base   3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d
head   f8e57e5d6b998ad04ac04a20945497ea174a9f2c
tree   4b5171c9c0b25432f692974ebdb04f6c76d17556
branch VXG-112226-chatgpt-pr518-runtime-continuity-composition-c001
paths  8
```

Changed paths:

```text
src/jvmMain/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoff.kt
src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt
src/jvmMain/kotlin/vexlife/service/VexLifeProdMacServiceLifetime.kt
src/jvmMain/kotlin/vexlife/setup/VexLifeProdSetupProcess.kt
src/jvmTest/kotlin/vexlife/local/VexLifeProdMacRuntimeHandoffTest.kt
src/jvmTest/kotlin/vexlife/service/VexLifeProdLocalServiceTest.kt
src/jvmTest/kotlin/vexlife/service/VexLifeProdMacServiceLifetimeTest.kt
src/jvmTest/kotlin/vexlife/setup/VexLifeProdSetupProcessTest.kt
```

## SourceOwner claim

The Product-owned canonical Mac candidate proof must pass on these exact bytes:

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

That proof owns repository/build correctness only. It does not launch the Product, perform real
model inference, mutate Home/Conversation/Memory, approve, READY, merge, or claim lived cache reuse.

On PASS, next is fresh Independent Assurance on exact #525 head. Only after source/assurance closure
does lived Warm Boot return to the same-runtime cache predicate.

## Human contract

```text
one exact Bash command
-> one canonical RETURN ZIP in ~/Downloads
-> return that ZIP
```

No alternate log bundle or manual branch selection is part of the ordinary path.

<!-- [VXG RealForever][PR525][SOURCEOWNER-A001][RELAY-COMPANION][DO-NOT-MERGE] -->
