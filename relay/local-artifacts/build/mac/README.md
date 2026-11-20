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
      Product #414 head bc78c8b52fd49cfa0bfc4a67dcbdffec9608f252
      RETURN sha256 fb5fc7f3fc9d3efbf4d0050aefb4cde80e39fee75bdea7a6042594f47aa7ae1c
      930 JVM tests / 6 failures
      no DMG formed
      no Product/Home/model mutation

A002  CURRENT
      source correction PR #433
      head 6f8f0509c015ef2509362b058b1610f02c3de25f
      tree 848e2902c374454854b5ce7f0c15ef6fb4e768b4
```

## Current host carrier

```text
contract      source.vexrelay.current-contract/v1
host          macos-victor
SDK membrane  vgong24/Vextreme-SDK#1959
carrier head  36eb5c82becd97dcfcbc8fa67455a74d8540503a
carrier tree  bea091ed67793931b838c7b412d9abce39a2e373
```

A002 executes:

```text
exact #433 source
-> jvmTest + checkVexLifeStrings
-> generateVexLifeProdScreenLens
-> require committed Lens byte-current
-> archiveVexLifeProdMacApplication
-> verify exact source-bound app / DMG / receipt
-> retain DMG evidence in canonical RETURN
-> consume RETURN locally
-> deliver verified DMG + byte-identical RETURN to ~/Downloads
```

Boundary:

```text
PROOF_COMPANION != PRODUCT_SOURCE_OWNER
A001 FAILURE != ERASED HISTORY
BUILD PASS != LIVED FEATURE PASS
BUILD PASS != READY
BUILD PASS != MERGE
```

<!-- [VXG RealForever][VEXLIFE-LOCAL-ARTIFACTS][TEMP-RELAY][DO-NOT-MERGE] -->
