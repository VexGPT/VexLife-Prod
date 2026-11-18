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


## LIVED-A002 consumed result

Victor returned:

```text
RETURN-t251vc-lived-a002--sha256-c4a0faf98c19ab7c5340c70bc5557dca4f140c19ed26f2cc1bacd5765f27cb90.zip
outer sha256 c4a0faf98c19ab7c5340c70bc5557dca4f140c19ed26f2cc1bacd5765f27cb90
outer disposition PASS

inner sha256 d1d4eec0ad32d69b3347576c75a969a2a48c6009febeb986be6a75779a6271f3
inner disposition SAFE_FAILURE
```

The Relay envelope was accepted and the exact #251 candidate checked out at the expected head/tree.
The temporary lived Kotlin harness compiled and the single lived JVM test ran.

The first substantive failure was:

```text
UNACCEPTED_EXIT_CODE:1
VexContextLivedBrowserProofTest
line 307: browser driver exitCode != 0
```

The RETURN proves the browser driver stopped but does not preserve the driver's own stdout/stderr
message because Gradle captured it behind the assertion. Therefore the exact browser failure class is
not yet grounded.

Classification:

```text
PRODUCT SOURCE FAILURE     not established
RELAY/CARRIER FAILURE      no
LIVED BROWSER HARNESS      stopped inside browser driver
EXACT BROWSER CAUSE        not preserved -> must diagnose, not guess
```

The candidate remained clean before/after and no real Vex Home/model/service/Memory mutation occurred.

Successor LIVED-A003 changes only proof observability: preserve browser-driver.log and JUnit XML on
failure, emit explicit Chrome/CDP/page/profile stage checkpoints, then re-run the same exact candidate.
