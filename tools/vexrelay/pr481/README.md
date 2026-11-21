# PR #481 Mac Relay + DMG proof companion

[VXG RealForever]

This directory preserves the exact human-carriage harness for VexLife-Prod PR #481.

## Why this exists

The observed recurring failure was not Product semantics; it was repeatedly rebuilding the local
proof/DMG carriage from chat. This companion keeps the working packet scope source-addressable.

References:
- VexLife-Prod #166 — one Mac carrier command -> local Relay -> DMG -> RETURN ZIP.
- VexLife-Prod #473 — current source-controlled formation + ExactHeadSourceOwnerProof + same-head lived DMG retention.
- current Relay contract: `source.vexrelay.current-contract/v1`.

## Boundary

```text
Local Workspace           NOT USED
GitHub Actions proof      NOT USED
Product source mutation   NOT PERFORMED
Home/model/runtime effect NOT PERFORMED

one digest-bound human ZIP
-> one exact Bash command
-> current SDK source acquired and verified
-> ExactHeadSourceOwnerProof on PR #481
-> only on PASS: same-head DMG formed + hash/hdiutil/codesign/receipt verified
-> one human RETURN ZIP containing Relay evidence + DMG
```

The `workspace` object inside the inner `vex.relay.task/v2` manifest is Relay's disposable RUN_ROOT;
it is not the Local Source Workspace subsystem.

## Exact candidate

```text
base   ae4a511b49ad4720b5c51de55ccb834d43aef218
head   40c6781f0920823f4a19231b33f7d4323cacac08
tree   637a4021e12e1133b2b2b2dfef5b38ad1cd4bdc5
branch VXG-112126-companion-engagement-bar-a001
```

## Current SDK binding

```text
head          6f8f6f50467f5df5a542713f7dc25b82db4ac8fa
tree          96795640b01f1969da980f52d5a8ec136a8c44aa
contract blob b53cbac01c2deeb45fad5c32ce98e0796c04126f
contractRef   source.vexrelay.current-contract/v1
runtime       v0.2.0-RC1
```

## Re-forming the human packet

Run `FORM-DOWNLOAD-ZIP.py` from this directory. It writes a deterministic digest-bound ZIP and prints
the exact one-line Mac command. The host packet contains only the three execution sources plus
`PACKET-MANIFEST.sha256`.

No merge/READY/release authority is implied by a proof PASS. Victor's lived UI/end-to-end observation
begins only after the returned DMG is available.
