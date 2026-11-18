# VexLife Preview publication carrier A002

[VXG RealForever]

A001 correctly stopped before publication because VexLife-Prod main advanced before the host effect.

A002 is the fresh immutable successor for current main:

- repository: `VexGPT/VexLife-Prod`
- branch: `main`
- source: `36055a912ed8ee8706822a0643c32a7976574203`
- tree: `730988455d6b201cf5a36f784eef5b1d89d7a914`
- prior A001 result: `SAFE_FAILURE / EXACT_CURRENT_MAIN / no publication`

The payload remains the same direct VexLocalBridge publication logic and invokes the Product-owned
`tools/release/publish-mac-release.sh preview`, followed by independent GitHub Release and
`gh-pages/data/releases.json` readback.

This branch is carrier-only and must not merge into Product main.
