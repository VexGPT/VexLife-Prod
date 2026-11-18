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
