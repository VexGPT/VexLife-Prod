# A009 — final transcript bridge + JSON route diagnostic

[VXG RealForever]

A008 materially advanced the lived circuit:

```text
Settings recovery      PASS by human witness
Local ASR READY        PASS by human witness
mic permission         PASS
Listening/capture      PASS
Stop                   PASS
final Composer text    FAIL
```

Source inspection found the final-result bridge defect: stdin whisper-cli disables the ordinary
segment callback and the Product did not request an explicit text output format. The corrected
Product source adds `-otxt` and a focused test preserving stdin + stdout transcript behavior.

## Exact Product candidate

```text
source PR  #245
head       58ad36f93f07b4e67c1972ae3027fb7661abb128
```

## A009 additional evidence

A009 also probes core localhost routes without retaining successful response bodies. If a route
returns non-2xx, only that service error body is kept. This is intended to identify which route is
behind the recurring misleading `REQUEST_JSON_INVALID` snackbar without collecting Conversation
content.

The host runner exits and packages immediately if the exact current source records
`LOCAL_ASR_INFERENCE_FAILED`; it no longer waits the full lived window after a known failure.

## Human boundary

Run exactly:

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=6011305ef094de8cab390f8e20dc5501d11ea82d 'repos/VexGPT/VexLife-Prod/contents/relay/local-asr/A009/RUN-MAC.command')
```

Expected result:

```text
~/Downloads/RETURN-VEXLIFE-LOCAL-ASR-PRODUCT-CIRCUIT-A009.zip
```

The RETURN contains no microphone audio or transcript text.

<!-- [VXG RealForever][PR245][TEMP-HOST-PROOF][LOCAL-ASR][A009] -->
