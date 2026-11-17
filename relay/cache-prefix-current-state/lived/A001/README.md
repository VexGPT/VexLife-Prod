# PR #230 lived cache/replay comparison carriage

[VXG RealForever]

This companion branch contains only the human-facing Mac startup/measurement carriage for the exact
Product candidate in PR #230. It does not change Product semantics.

## Exact Product candidate

```text
repository VexGPT/VexLife-Prod
base       d0cba51decfe9057ac9afc007fdeb37da213cc97
head       47decbf10bdb53a33afc21affc1080bcf2b125fd
tree       110e11ee1b0bd2b30e400455bb935dc2a25a6433
PR         #230
```

Permanent distinction:

```text
PRODUCT PROOF CANDIDATE
!=
LIVED MAC CARRIAGE
```

## Why this bundle exists

PR #230 has exact SourceOwner/source-review evidence and fresh semantic Assurance CLEAR on the head
above. The remaining claim is lived compute behavior: whether request-current placement after exact
Conversation materially improves Qwen prompt-cache reuse without disturbing the already-working
Conversation / same-turn reach / runtime circuit.

The bundle deliberately reuses the predecessor's accepted PR #211 human path:

```text
exact candidate source
-> source-owned archiveVexLifeProdMacApplication
-> verified DMG
-> existing Home / existing serving model
-> isolated Product diagnostics
-> lived turns
-> one Product-owned debug ZIP
```

## Bundle

```text
VexLife-PR230-Lived-Cache-Comparison-A001--sha256-f04c7d3bb5ea667f5d1cef5f5e9c02480fd65d4957ba17497b01d6b1bc7b7d1f.zip

sha256
f04c7d3bb5ea667f5d1cef5f5e9c02480fd65d4957ba17497b01d6b1bc7b7d1f
```

Members:

```text
RUN.command
README.txt
```

RUN.command acquires and verifies the exact candidate, builds the Product-owned Mac application/DMG,
verifies checksum + hdiutil + bundle/source/icon identity, and leaves:

```text
~/Downloads/VexLife-PR230-47decbf1/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  START-HERE.txt
  COLLECT-COMPARISON.command
```

START-HERE preserves the predecessor's three ordinary baseline prompts, then adds one explicit real
web-fetch turn and one no-new-fetch continuation. The Product's own diagnostics are cleared before the
run, enhanced capture is bounded by the Product, and the collector uses the existing
`/api/v1/diagnostics/export` owner.

No source mutation, merge, release publication, Home recreation, model switch, cache-policy change or
unrelated lifecycle effect is admitted by this carriage.

<!-- [VXG RealForever][VEXLIFE-PROD][PR230][LIVED-CACHE-COMPARISON][A001] -->
