# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Attempt history

```text
A001  TERMINAL SAFE_FAILURE
      AppleScriptObjC launch parser collision

A002  TERMINAL SAFE_FAILURE
      false Swift-unavailable classification

A003  TERMINAL SAFE_FAILURE
      macOS-unavailable WebKit callback + Swift @main parse mode

A004  TERMINAL SAFE_FAILURE
      duplicate NSMicrophoneUsageDescription insertion after osacompile

A005  PAYLOAD PASS / CARRIER FINALIZATION FAILED
      exact Product build formed and verified
      retained evidence was placed under transient bridge/work and removed before RETURN packaging

A006  TERMINAL PASS
      recovered A005 build without rebuilding
      canonical RETURN retained exact DMG
      human first-run inspection then exposed AppKit runtime constant resolution defect

A007  CURRENT
      Product head 3fe20be5d4c5f80ff5f9e066862e971b325159e1
      Product tree 0eb4db2c0493285f65bbfa3660a32dde2a51385c
```

A001-A006 are immutable historical evidence. Do not replay them.

A007 rebuilds because lived inspection changed Product source:
- AppKit progress-window style masks are resolved from `current application`;
- AppKit backing-store constant is resolved from `current application`;
- regression coverage forbids the lived-failing unqualified constant form;
- Product semantics, Home/Conversation/Memory ownership, close-vs-quit semantics and WebKit presenter meaning remain unchanged.

## Meaning

This companion **builds only**. It does not claim SourceOwner PASS,
application-lived PASS, Product-Quit PASS, READY, release, or merge.

Expected PASS delivery:

```text
~/Downloads/VexLife-WebToApp-Mac-3fe20be5/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A007-CURRENT][LIVED-RUNTIME-REPAIR][DO-NOT-MERGE] -->
