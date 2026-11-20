# Web Restoration Mac A004 — read-only Java selection inspection

[VXG RealForever]

This attempt exists to answer one unresolved fact from the A003 lived entry failure:

```text
Which executable path did the unchanged Setup selector choose for A003,
and which exact selector predicate produced that choice?
```

It does **not** change Product source and it does **not** test Voice.

## Bound evidence

```text
Product candidate
  source f38779503f9a1171e26593f3542f2477c6e2370a
  tree   6a5b0b0d4730fb8e0078b42f083677469b6ea63a

A003 delivered DMG
  ~/Downloads/VexLife-Web-Restoration-f3877950.dmg
  sha256 cb79560458571f898e65d3b3ce6cd192e025cddfbea007f6e7c3555171199f0e
```

The payload reads the current machine/legacy service lifetime receipts, mounts that exact DMG read-only into the task-owned temporary root, reads its embedded current `service-image.json`, and recomputes the AppleScript selector in source order:

```text
machine receipt
-> legacy receipt
-> bundled A003 setup launcher fallback
```

For each receipt candidate it records the exact first rejection predicate, including schema, Home/image coordinate, source/payload, platform, Java major, runtime-module subset, and executable-bit checks. It then observes xattr/codesign/spctl state for the selected Java path without executing it.

## Held

```text
Product application launch
Java execution
Whisper execution
Voice semantics changes
stable-provider custody changes
Windows changes
Language changes
browser-currentness changes
Home/service/runtime mutation
quarantine mutation
process termination
publication / READY / merge
```

A004 PASS means only that the selector question was observed from the exact A003 artifact and current preserved host state. It does not mean the selected Java will successfully execute, nor that Mac Voice is restored.

<!-- [VXG RealForever][VEXLIFE-PROD][PR436][JAVA-SELECTION][A004][READ-ONLY] -->
