# VexLife Local Artifacts — Mac build / DMG proof companion

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge proof-carriage companion for the
[VexLife Local Artifacts] source stack.

Product meaning remains on #414 and its proof-driven correction child #433. This branch carries only
source-controlled Relay formation, host execution, canonical RETURN consumption, and verified DMG
delivery.

## Attempt state

```text
A001  SAFE_FAILURE / immutable history
      #414 source: 930 tests / 6 failures

A002  SAFE_FAILURE / immutable history
      #433 correction: 930 tests / 2 failures

A003  CARRIER_FAILURE_AFTER_PAYLOAD_PASS / immutable history
      #433 head 1b91ff956a394e4f786209020e435fb4afe64c36
      payload exit 0
      JVM/string PASS
      Screen Lens byte-current PASS
      Mac app + DMG formation/verification PASS
      carrier rejected retained target namespace:
        retained/local-artifacts/*
      required:
        retained/inner-vexrelay/*

A004  CURRENT / RECOVERY WITHOUT REBUILD
      recover exact A003 preserved build
      re-verify exact source + payload PASS + app + DMG + receipt
      package retained evidence with carrier-required namespace
```

## Current carrier rule exposed by A003

```text
retainedEvidenceFiles[].targetPath
MUST start with
retained/inner-vexrelay/
```

A004 changes no Product source and reruns no Gradle build.

## Current source binding

```text
source PR   #433
head        1b91ff956a394e4f786209020e435fb4afe64c36
tree        2870384635d82f96d4a0c0ae93f6c69e850855bb
```

## Recovery source

```text
/Users/vexlife/Downloads/.vexlife-pr429-local-artifacts-build-a003.jDSReO
```

The recovery task fails closed unless the preserved source head/tree, A003 payload PASS evidence,
DMG/source binding, signature, DMG verification, archive receipt and SHA sidecar all agree.

Boundary:

```text
A003 PAYLOAD PASS != A003 CANONICAL RETURN
A004 RECOVERY != REBUILD
PROOF_COMPANION != PRODUCT_SOURCE_OWNER
BUILD PASS != LIVED FEATURE PASS
BUILD PASS != READY
BUILD PASS != MERGE
```

<!-- [VXG RealForever][VEXLIFE-LOCAL-ARTIFACTS][TEMP-RELAY][A004-RECOVERY][DO-NOT-MERGE] -->
