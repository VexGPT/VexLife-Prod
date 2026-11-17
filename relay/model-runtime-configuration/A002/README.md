[VXG RealForever]

# VexLife-Prod model/runtime configuration host proof — A002

Companion Relay carriage for Product Draft PR #238.

## Product proof candidate

```text
repository  VexGPT/VexLife-Prod
PR          #238
base        47decbf10bdb53a33afc21affc1080bcf2b125fd
head        51563346e5a7a7acbf78b9a5f89135f9bfadf687
tree        52d93d4d569685688c1b583bff8996b002f16ea7
branch      VXG-111726-model-runtime-configuration-a001
changed     exactly 20 paths
```

This companion branch is carriage/history only. It does not modify the Product candidate above.

## A001-R2 formation failure

Victor ran A001-R2 on the Mac. SDK source cloned and grounded correctly, but task formation stopped before
a VexRelay task existed:

```text
MANIFEST_REVIEW_FILE_CAP:operations[5]
```

Classification: **Relay formation harness error, not Product source failure**.

Exact cause from SDK `8543c7d755faba04d4c4019c02e84f992667918a`:

- `ExactHeadSourceOwnerProof` emits one `REVIEW_SNAPSHOT`;
- manifest validation admits `maxFiles` only in `1..10`;
- A001-R2 set `stage.maxFiles` to the Product's 20 changed paths.

No Product tests, build, Home effect, model effect, runtime effect, or source mutation occurred.

## A002 correction

A002 sets:

```text
stage.maxFiles = 10
```

and preserves the complete Product claim:

- `source.expectedChangedPaths` remains all 20 exact paths;
- repository membrane remains exactly those 20 paths;
- exact base/head/tree remain unchanged;
- `candidate.diff` and `allChangedPaths` cover the complete base..head candidate;
- review projection returns up to 10 source bodies and explicitly records the remaining paths as
  `MAX_FILE_COUNT` omissions;
- semantic clearance remains a separate Independent Assurance gate.

This follows the current VexRelay contract rather than widening its review cap.

## Stable formation bundle

```text
VexLife-Model-Runtime-Configuration-RelayFormation-A002--sha256-bb3a4faa4ee543bc44efa62fd4d5e3002479af08fc2372b00d5eb9f450c8250b.zip
sha256 bb3a4faa4ee543bc44efa62fd4d5e3002479af08fc2372b00d5eb9f450c8250b
```

The committed ZIP is stable formation source. The canonical time-bounded inner VexRelay / outer
VexLocalBridge task is formed fresh on Victor's Mac.

## Local formation validation

Before commit:

```text
/bin/bash -n RUN.command        PASS
node --check formation.mjs      PASS
formation maxFiles=10           PASS
all 20 expectedChangedPaths     PRESERVED
all 20 repository membrane      PRESERVED
PR/head/tree exact binding      PRESERVED
bundle SHA-256                  MATCH
```

<!-- [VXG RealForever][VEXLIFE-PROD][PR238][HOST-PROOF][A002] -->
