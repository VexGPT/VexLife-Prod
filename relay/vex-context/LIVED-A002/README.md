# Temporary Relay carriage for PR #251 — lived Vex Context browser A002

[VXG RealForever]

LIVED-A001 was rejected by the current VexRelay manifest validator **before any Product/browser proof
ran** because its on-pass route used a descriptive but non-canonical lifecycle label:

```text
LIVED_UI_PROVEN
```

The current accepted lifecycle vocabulary requires one of the source-owned states. A002 changes only
the Relay envelope:

```text
currentLifecycleState  ASSURANCE_PENDING
onPass.lifecycleState  ASSURANCE_EVIDENCE_READY
```

No #251 Product source changed.

## Exact candidate

```text
PR     #251
base   64c5be05a2e4331de86672d1285923f175e88e9f
head   4ecf2d3c0fb21f9958455313148b7e04eeb759fd
tree   dbf3ec8511c99ee940fd78eecebcd3963eada495
paths  22
```

## Lived membrane

Same as A001:

- disposable canonical Home only;
- Product `VexLifeProdLocalService.startForTest` on random loopback ports;
- one canonical scripted DirectCompanionTurn;
- real generated VexWeb in headless Chrome;
- no real `~/.vexlife` read/mutation;
- no real model call;
- no ordinary Product service rotation;
- no Memory semantic mutation;
- no approval / READY / merge / publication.

## Formation bundle

```text
VexLife-Vex-Context-LivedBrowser-RelayFormation-A002--sha256-983c7ccd215b53e16c3c194758edbdec993429331fe1dc62efe35d7ce12f01ba.zip
sha256 983c7ccd215b53e16c3c194758edbdec993429331fe1dc62efe35d7ce12f01ba
bytes  25625
members RUN.command, formation.mjs, relay-context.json, README.txt
```

Formation checks:

```text
bash -n RUN.command PASS
node --check formation.mjs PASS
relay-context.json parse PASS
attemptRef / nonce / outer attempt / result filename advanced to A002
onPass.lifecycleState = ASSURANCE_EVIDENCE_READY
```

Upload the exact canonical:
`RETURN-VEXLIFE-VEX-CONTEXT-LIVED-BROWSER-A002--sha256-<digest>.zip`
even on SAFE_FAILURE.

<!-- [VXG RealForever][PR251][PR261][LIVED-BROWSER][A002] -->
