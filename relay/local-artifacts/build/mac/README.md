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
      source #414
      930 tests / 6 failures
      RETURN fb5fc7f3fc9d3efbf4d0050aefb4cde80e39fee75bdea7a6042594f47aa7ae1c

A002  SAFE_FAILURE / immutable history
      source #433 head 6f8f0509c015ef2509362b058b1610f02c3de25f
      930 tests / 2 failures
      RETURN dfc42e8fa739245379c28f31db875cc0b4ea784aa3b4e5d16594facbe91ced95

A003  CURRENT
      source #433
      head 1b91ff956a394e4f786209020e435fb4afe64c36
      tree 2870384635d82f96d4a0c0ae93f6c69e850855bb
```

A001/A002 remain durable causal evidence and are never replayed or overwritten.

## Current host carrier

```text
contract      source.vexrelay.current-contract/v1
host          macos-victor
SDK membrane  vgong24/Vextreme-SDK#1959
carrier head  36eb5c82becd97dcfcbc8fa67455a74d8540503a
carrier tree  bea091ed67793931b838c7b412d9abce39a2e373
```

A003 executes:

```text
exact #433 source
-> jvmTest + checkVexLifeStrings
-> generateVexLifeProdScreenLens
-> require committed Lens byte-current
-> archiveVexLifeProdMacApplication
-> verify exact source-bound app / DMG / receipt
-> retain DMG evidence in canonical RETURN
-> copy canonical RETURN to named Downloads folder before consumption
-> consume RETURN locally
-> PASS: deliver verified DMG beside RETURN
-> SAFE_FAILURE: RETURN remains available for continuation
```

Boundary:

```text
PROOF_COMPANION != PRODUCT_SOURCE_OWNER
A001/A002 FAILURE != ERASED HISTORY
BUILD PASS != LIVED FEATURE PASS
BUILD PASS != READY
BUILD PASS != MERGE
```

<!-- [VXG RealForever][VEXLIFE-LOCAL-ARTIFACTS][TEMP-RELAY][DO-NOT-MERGE] -->
