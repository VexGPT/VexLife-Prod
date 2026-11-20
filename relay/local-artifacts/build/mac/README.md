# VexLife Local Artifacts — Mac build / DMG proof companion

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge proof-carriage companion for the
[VexLife Local Artifacts] source stack.

Product meaning remains on #414 and its proof-driven correction child #433. This branch carries only
source-controlled Relay formation, host execution, canonical RETURN consumption, and verified DMG
delivery.

## Attempt state

```text
A001  SAFE_FAILURE
      #414: 930 tests / 6 failures

A002  SAFE_FAILURE
      #433: 930 tests / 2 failures

A003  PRODUCT/BUILD PAYLOAD PASS; CARRIER PACKAGING FAILURE
      exact #433 source
      jvmTest + strings PASS
      Screen Lens byte-current PASS
      app + DMG formation and verification PASS
      carrier rejected retained/local-artifacts/*

A004  RECOVERY HARNESS FAILURE
      looked for A003 payload-result in carrier ephemeral work custody
      that work root had already been cleaned
      preserved A003 source/build remained intact

A005  CURRENT
      recover exact A003 source/build
      re-earn jvmTest + strings and Screen Lens currentness
      DO NOT rebuild app/DMG
      re-verify existing app/DMG/receipt
      package under retained/inner-vexrelay/*
```

## Source

```text
#433
branch VXG-111926-vexlife-local-artifacts-dmg-lived-proof-a001
head   1b91ff956a394e4f786209020e435fb4afe64c36
tree   2870384635d82f96d4a0c0ae93f6c69e850855bb
```

## A005 recovery source

```text
/Users/vexlife/Downloads/.vexlife-pr429-local-artifacts-build-a003.jDSReO
```

A005 does not depend on deleted A003 carrier work custody. It re-earns the source gates directly on
the preserved exact source and independently verifies the already-built artifact.

Retained evidence uses the exact #1959 carrier namespace:

```text
retained/inner-vexrelay/local-artifacts/
```

Boundary:

```text
A005 GATE RERUN != DMG REBUILD
PROOF_COMPANION != PRODUCT_SOURCE_OWNER
BUILD PASS != LIVED FEATURE PASS
BUILD PASS != READY
BUILD PASS != MERGE
```

<!-- [VXG RealForever][LOCAL-ARTIFACTS][A005][RECOVERY-WITHOUT-REBUILD][DO-NOT-MERGE] -->
