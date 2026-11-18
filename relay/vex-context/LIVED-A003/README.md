# Vex Context lived-browser LIVED-A003 — GitHub-carried diagnostic attempt

[VXG RealForever]

## Why A003 exists

LIVED-A002 reached the exact #251 checkout, compiled the disposable Kotlin lived harness, and ran the
single lived JVM test. The browser driver returned exit code 1, but the RETURN did not preserve the
driver's own stdout/stderr before Gradle surfaced only the assertion failure.

A003 changes **proof observability only**. Product PR #251 bytes are unchanged.

## Exact Product candidate

```text
repository  VexGPT/VexLife-Prod
PR          #251
base        64c5be05a2e4331de86672d1285923f175e88e9f
head        4ecf2d3c0fb21f9958455313148b7e04eeb759fd
tree        dbf3ec8511c99ee940fd78eecebcd3963eada495
```

## Diagnostic delta

- preserve `browser-driver.log` before the Kotlin assertion;
- on Gradle failure, print that log and JUnit XML into canonical Relay execution evidence;
- emit `LIVED_STEP` checkpoints for Chrome discovery/start, DevTools/CDP, page load, Save As,
  last-turn-vs-next-turn, process-view locality, export, restore, import, and View in Vex Home;
- keep the disposable-Home / no-real-model / no-ordinary-service-rotation membrane unchanged.

## GitHub-carried formation bytes

The exact formation ZIP is carried in this companion PR as six ordered base64 parts:

```text
bundle.b64.part01
bundle.b64.part02
bundle.b64.part03
bundle.b64.part04
bundle.b64.part05
bundle.b64.part06
```

Concatenate in lexical order, base64-decode, then require:

```text
filename  VexLife-Vex-Context-LivedBrowser-RelayFormation-A003--sha256-8b9e8831b0b2347fe7e386166598f9891671bdd5bc194de71709b776fd5d59ac.zip
sha256    8b9e8831b0b2347fe7e386166598f9891671bdd5bc194de71709b776fd5d59ac
bytes     26585
```

This encoding keeps the exact digest-bound package in the GitHub companion lane even where the
connector cannot write binary repository blobs directly.

## Human handoff

The continuing chat supplies one command pinned to the exact companion commit. That command:

1. fetches these six files from GitHub;
2. reconstructs the ZIP;
3. verifies the SHA-256 and byte count;
4. extracts it into a temporary Downloads directory;
5. runs `RUN.command`;
6. leaves only the canonical RETURN for Victor to send back.

No separate ChatGPT artifact download is required.

Expected return:

`RETURN-VEXLIFE-VEX-CONTEXT-LIVED-BROWSER-A003--sha256-<digest>.zip`

<!-- [VXG RealForever][PR251][PR261][LIVED-BROWSER][A003][GITHUB-CARRIED] -->


## GitHub carriage correction

Victor's first GitHub-native launcher correctly failed closed on SHA-256 mismatch:

```text
expected 8b9e8831b0b2347fe7e386166598f9891671bdd5bc194de71709b776fd5d59ac
observed c8bd7d44db8721e324b10e9d83b71bffe60c43a49f59970c31e655ac4e771b82
```

The failure occurred before ZIP extraction or Relay execution.

Root cause was one manually-carried base64 character in `bundle.b64.part04`:

```text
expected  ...H8FXZusF+...
carried   ...H8FXzusF+...
                  ^
```

The part is corrected in GitHub. The combined carried base64 now matches the original expected length
(`35448` characters) and the original per-part + combined rolling checksums. The human launcher still
requires the authoritative decoded ZIP SHA-256 and byte count before execution, so this bookkeeping
repair cannot bypass the cryptographic gate.

No Product source changed.


## Preferred GitHub handoff

For human execution, use the single consolidated `bundle.b64` file in this directory. The six
`bundle.b64.partNN` files remain as historical/debug carriage evidence only.

`bundle.b64` is the corrected concatenation of those parts and is the preferred one-fetch path for
the pinned launcher.


## LIVED-A003 consumed result

Victor returned:

```text
RETURN-t251vc-lived-a003--sha256-d04fe60b53dea7bf71b67ec0bc8ae835703e1072dc4e1ce5fa60011a263d7314.zip
outer disposition PASS
outer sha256 d04fe60b53dea7bf71b67ec0bc8ae835703e1072dc4e1ce5fa60011a263d7314

inner disposition SAFE_FAILURE
inner sha256 4d63fa5379cc983bd1f41aae4720c2a70ba9c519460a3e63f6dbebd01bfaabd3
```

A003 successfully preserved the first browser-driver failure:

```text
HEADLESS_CHROME_NOT_FOUND
```

Before that edge:

- exact #251 head/tree checkout PASS;
- temporary Kotlin harness compiled;
- canonical scripted Vex turn completed;
- VexLogger observed SEND_ACCEPTED -> PROVIDER_INVOCATION_REQUESTED -> PROVIDER_STAGE_OBSERVED ->
  TURN_COMPLETED -> WORKING_CONTINUITY_COMMITTED;
- repository remained exact-clean;
- real ~/.vexlife/model/runtime/ordinary service remained untouched.

Classification: **host lived-render dependency absent**, not Product source failure.

Successor LIVED-A004 replaces the Chrome/CDP proof driver with macOS-native WebKit/WKWebView. Product
bytes remain unchanged.
