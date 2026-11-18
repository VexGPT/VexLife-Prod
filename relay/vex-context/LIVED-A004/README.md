# Vex Context lived-browser LIVED-A004 — host-native WebKit proof

[VXG RealForever]

LIVED-A003 reached the exact #251 candidate and completed the canonical scripted Vex turn, then
stopped at `HEADLESS_CHROME_NOT_FOUND`. That is a host render dependency absence, not a Product
failure.

A004 changes the **proof renderer only**:

```text
Google Chrome / Chromium + CDP
        ↓ replaced by
macOS AppKit + WKWebView
```

No #251 Product bytes changed.

## Exact Product candidate

```text
repository  VexGPT/VexLife-Prod
PR          #251
base        64c5be05a2e4331de86672d1285923f175e88e9f
head        4ecf2d3c0fb21f9958455313148b7e04eeb759fd
tree        dbf3ec8511c99ee940fd78eecebcd3963eada495
```

## Lived membrane

A004 uses:

- disposable canonical Vex Home only;
- `VexLifeProdLocalService.startForTest` on random loopback ports;
- one canonical scripted `DirectCompanionTurn`;
- the actual generated VexWeb;
- macOS-native `WKWebView` / AppKit rendering and JavaScript execution;
- WebKit snapshot output for visual evidence.

It must not:

- read or mutate Victor's real `~/.vexlife`;
- call the real model;
- rotate the ordinary Product service;
- mutate Memory semantic authority;
- approve, READY, merge, or publish.

The lived circuit still covers profile Save As / Save revision / restore / export / import,
process-view locality, View in Vex Home against the disposable Home, Product Foundation visibility,
and the distinction between the last completed turn's receipt and the profile selected for the next
turn.

## GitHub-native exact carrier

The actual binary formation ZIP is committed directly beside this README:

```text
VexLife-Vex-Context-LivedBrowser-RelayFormation-A004--sha256-ff5ef35b9998805feb074c89c0212a0001ceb04c51a7e78df1176f9c4d379133.zip
sha256 ff5ef35b9998805feb074c89c0212a0001ceb04c51a7e78df1176f9c4d379133
bytes  25451
```

The human launcher must fetch this exact file from a **pinned companion commit**, verify SHA-256 and
byte count, then run `RUN.command`.

Formation-side checks completed before carriage:

```text
bash -n RUN.command     PASS
node --check formation PASS
relay-context JSON     PASS
swiftc -parse driver   PASS
```

Expected canonical return:

`RETURN-VEXLIFE-VEX-CONTEXT-LIVED-BROWSER-A004--sha256-<digest>.zip`

Send the RETURN even on SAFE_FAILURE.

<!-- [VXG RealForever][PR251][PR261][LIVED-BROWSER][A004][WEBKIT][GITHUB-BINARY] -->
